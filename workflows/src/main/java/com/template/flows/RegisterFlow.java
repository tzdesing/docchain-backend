package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.template.contracts.RegisterContract;
import com.template.states.Register;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.*;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class RegisterFlow extends FlowLogic<SignedTransaction> {

    private Register register;

    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new BatchTransaction.");
    private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
    private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };

    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
    );

    public RegisterFlow(Register register) {
        this.register = register;

    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        ServiceHub serviceHub = getServiceHub();
        List<StateAndRef<Register>> statesFromVault = serviceHub.getVaultService().queryBy(Register.class).getStates();

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //Stage 1
        // Generating Unsigned Transaction
        //UUID uuid = UUID.fromString(register.getId());
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                null,
                Collections.singletonList(register.getId()),
                Vault.StateStatus.UNCONSUMED);//CONSUMED,UNCOSUMED,ALL

        Party me = getOurIdentity();

        Register tempState = new Register(new UniqueIdentifier(register.getId(), UUID.randomUUID()),
                register.getId(), register.getPayload(), me, register.getToUser());

        List<StateAndRef<Register>> listStateAndRef = getServiceHub().getVaultService().
                queryBy(Register.class, queryCriteria).getStates();

        System.out.println(Arrays.toString(listStateAndRef.toArray()));

        //if exists() update data
        if (listStateAndRef.size() > 0) {
            tempState = listStateAndRef.get(0).getState().getData();;
            tempState.setPayload(register.getPayload());
            tempState.setFromUser(me);
            tempState.setToUser(register.getToUser());
        }

        final Command<RegisterContract.Commands.Register> txCommand = new Command<>(
                new RegisterContract.Commands.Register(),
                ImmutableList.of(tempState.getFromUser().getOwningKey(), tempState.getToUser().getOwningKey()));

        final TransactionBuilder txBuilder = new TransactionBuilder(notary);
        if (listStateAndRef.size() > 0) {
             txBuilder.addInputState(listStateAndRef.get(0))
                    .addOutputState(tempState)
                    .addCommand(txCommand);
        }else{
           txBuilder.addOutputState(tempState)
                    .addCommand(txCommand);
        }
        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        // Verify that the transaction is valid.
        txBuilder.verify(getServiceHub());

        // Stage 3.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        //sign the transaction
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Stage 4.
        progressTracker.setCurrentStep(GATHERING_SIGS);
        // Send the state to the counterparty, and receive it back with their signature.
        FlowSession ownerSession = initiateFlow(register.getToUser());
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(ownerSession), CollectSignaturesFlow.Companion.tracker()));

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(ownerSession)));
    }

    @InitiatedBy(RegisterFlow.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {
        private final FlowSession ownerSession;
        public Acceptor(FlowSession ownerSession) {
            this.ownerSession = ownerSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession ownerPartyFlow, ProgressTracker progressTracker) {
                    super(ownerPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an register transaction.", output instanceof Register);
                        return null;
                    });
                }
            }
            final SignTxFlow signTxFlow = new SignTxFlow(ownerSession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            return subFlow(new ReceiveFinalityFlow(ownerSession, txId));
        }
    }
}



