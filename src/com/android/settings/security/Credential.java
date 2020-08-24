/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.security.Credentials;

import java.util.EnumSet;

/**
 * Credential can represent either CA or User Certificate, depending on boolean constructor argument
 * Used in Certificates dashboard.
 * If CA, uid won't be used and userId specified.
 * If User, userId won't be used and uid specified.
 */
public class Credential implements Parcelable {
    public enum Type {
        CA_CERTIFICATE(Credentials.CA_CERTIFICATE),
        USER_CERTIFICATE(Credentials.USER_CERTIFICATE),
        USER_KEY(Credentials.USER_PRIVATE_KEY, Credentials.USER_SECRET_KEY);

        public final String[] prefix;

        Type(String... prefix) {
            this.prefix = prefix;
        }
    }

    /**
     * Main part of the credential's alias. To fetch an item from KeyStore, prepend one of the
     * prefixes from {@link Credential#storedTypes}.
     */
    public final String alias;

    /**
     * UID under which this credential is stored. Typically {@link Process#SYSTEM_UID} but can
     * also be {@link Process#WIFI_UID} for credentials installed as wifi certificates.
     * Equals {@link Process#INVALID_UID} if type is CA_CERTIFICATE.
     */
    public final int uid;

    /**
     * User Id under which this credential is stored.
     * Only used for {@link Credential.Type#CA_CERTIFICATE},
     * else it equals {@link UserHandle#USER_NULL}.
     */
    public final int userId;

    /**
     * Should contain some non-empty subset of:
     * <ul>
     *   <li>{@link Credential.Type#CA_CERTIFICATE}</li>
     *   <li>{@link Credential.Type#USER_CERTIFICATE}</li>
     *   <li>{@link Credential.Type#USER_KEY}</li>
     * </ul>
     */
    public final EnumSet<Credential.Type> storedTypes = EnumSet.noneOf(
            Credential.Type.class);

    /**
     * Creates either CA or User certificate
     * @param alias
     * @param userId used for CA certificates
     * @param uid used for User certificates
     */
    private Credential(String alias, int userId, int uid) {
        this.alias = alias;
        this.userId = userId;
        this.uid = uid;
    }

    /**
     * Called to create User Credential
     * @param alias
     * @param uid
     * @return initialised Credential object
     */
    public static Credential buildUserCredential(String alias, int uid) {
        return new Credential(alias, UserHandle.USER_NULL, uid);
    }

    /**
     * Called to create Trusted Credential (CA certificate)
     * @param alias
     * @param userId
     * @return initialised Credential object
     */
    public static Credential buildCaCertificate(String alias, int userId) {
        return new Credential(alias, userId, Process.INVALID_UID);
    }

    /**
     * Creates a User certificate using a Parcel
     * @param in
     */
    public Credential(Parcel in) {
        this(in.readString(), UserHandle.USER_NULL, in.readInt());

        long typeBits = in.readLong();
        for (Credential.Type i : Credential.Type.values()) {
            if ((typeBits & (1L << i.ordinal())) != 0L) {
                storedTypes.add(i);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes Credential to a given Parcel
     * @param out Parcel to be written to
     * @param flags Additional flags about how the object should be written.
     */
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(alias);
        out.writeInt(uid);

        long typeBits = 0;
        for (Credential.Type i : storedTypes) {
            typeBits |= 1L << i.ordinal();
        }
        out.writeLong(typeBits);
    }

    public static final Parcelable.Creator<Credential> CREATOR =
            new Parcelable.Creator<Credential>() {
                public Credential createFromParcel(Parcel in) {
                    return new Credential(in);
                }

                public Credential[] newArray(int size) {
                    return new Credential[size];
                }
    };

    /**
     * Checks whether User Credential is from system
     * @return
     */
    public boolean isSystem() {
        return UserHandle.getAppId(uid) == Process.SYSTEM_UID;
    }

    /**
     * Checks whether Credential is a Trusted CA certificate
     * @return boolean
     */
    public boolean isCA() {
        return userId != UserHandle.USER_NULL;
    }

    public String getAlias() {
        return alias;
    }

    public EnumSet<Credential.Type> getStoredTypes() {
        return storedTypes;
    }
}
