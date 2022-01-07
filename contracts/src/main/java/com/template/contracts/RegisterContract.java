package com.template.contracts;

import com.template.states.Register;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class RegisterContract implements Contract {

    public static String ID = "RegisterContract";
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        final CommandWithParties<Commands.Register> command = requireSingleCommand(tx.getCommands(), Commands.Register.class);
        requireThat(require -> {
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final Register out = tx.outputsOfType(Register.class).get(0);
            return null;
        });

    }

    public interface Commands extends CommandData {
        class Register implements Commands {}
    }



}