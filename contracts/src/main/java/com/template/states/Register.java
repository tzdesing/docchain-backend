package com.template.states;

import com.google.common.collect.ImmutableList;
import com.template.Schema.RegisterSchema;
import com.template.contracts.RegisterContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;


@BelongsToContract(RegisterContract.class)

@CordaSerializable
public class Register implements LinearState, QueryableState {

    private UniqueIdentifier registerId = null;
    private String id;
    private String payload;
    private Party fromUser;
    private Party toUser;

    public Register() {

    }

    public Register(UniqueIdentifier registerId, String id, String payload, Party fromUser, Party toUser) {
        this.registerId = registerId;
        this.id = id;
        this.payload = payload;
        this.fromUser = fromUser;
        this.toUser = toUser;
    }


    public UniqueIdentifier getRegisterId() {
        return registerId;
    }

    public String getPayload() {
        return payload;
    }
    public Party getFromUser() {
        return fromUser;
    }
    public Party getToUser() {
        return toUser;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public void setFromUser(Party fromUser) {
        this.fromUser = fromUser;
    }

    public void setToUser(Party toUser) {
        this.toUser = toUser;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() { return registerId; }

    @Override
    public String toString() {
        return "Register{" +
                "registerId=" + registerId +
                ", id='" + id + '\'' +
                ", payload='" + payload + '\'' +
                ", fromUser=" + fromUser +
                ", toUser=" + toUser +
                '}';
    }


    @NotNull
    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof RegisterSchema) {
            return new RegisterSchema.PersistentRegister(
                    this.registerId.getId(),
                    null,//id
                    null,//
                    this.fromUser,
                    this.toUser
            );
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new RegisterSchema());
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(fromUser, toUser);
    }

}