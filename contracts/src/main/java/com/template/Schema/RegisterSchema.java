package com.template.Schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.serialization.CordaSerializable;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@CordaSerializable
public class RegisterSchema extends MappedSchema{

    public RegisterSchema() {
        super(RegisterSchemaV.class, 1, ImmutableList.of(PersistentRegister.class));
    }

    @Entity
    @Table(name = "registers")
    public static class PersistentRegister extends PersistentState {

        @Column(name = "linear_id") private final UUID linearId;
        @Column(name = "id") private final String id;

        @Column(name = "payload") private final String payload;

        @Column(name = "fromUser") private final Party fromUser;
        @Column(name = "toUser") private final Party toUser;

        public PersistentRegister(UUID linearId, String id, String payload,
                                  Party fromUser, Party toUser) {

            this.linearId = linearId;
            this.id = id;
            this.payload = payload;
            this.fromUser = fromUser;
            this.toUser = toUser;
        }

        public PersistentRegister() {
            this.linearId = null;
            this.id = null;
            this.payload = null;
            this.fromUser = null;
            this.toUser = null;
        }

        public UUID getLinearId() {
            return linearId;
        }

        public String getId() {
            return id;
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
    }

}
