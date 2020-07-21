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

public class Credential implements Parcelable {
    public enum Type {
        CA_CERTIFICATE (Credentials.CA_CERTIFICATE),
        USER_CERTIFICATE (Credentials.USER_CERTIFICATE),
        USER_KEY(Credentials.USER_PRIVATE_KEY, Credentials.USER_SECRET_KEY);

        public final String[] prefix;

        Type(String... prefix) {
            this.prefix = prefix;
        }
    }

    /**
     * Main part of the credential's alias. To fetch an item from KeyStore, prepend one of the
     * prefixes from {@link CredentialItem.storedTypes}.
     */
    public final String alias;

    /**
     * UID under which this credential is stored. Typically {@link Process#SYSTEM_UID} but can
     * also be {@link Process#WIFI_UID} for credentials installed as wifi certificates.
     */
    public final int uid;

    /**
     * Should contain some non-empty subset of:
     * <ul>
     *   <li>{@link Credentials.CA_CERTIFICATE}</li>
     *   <li>{@link Credentials.USER_CERTIFICATE}</li>
     *   <li>{@link Credentials.USER_KEY}</li>
     * </ul>
     */
    public final EnumSet<Credential.Type> storedTypes = EnumSet.noneOf(
            Credential.Type.class);

    public Credential(final String alias, final int uid) {
        this.alias = alias;
        this.uid = uid;
    }

    public Credential(Parcel in) {
        this(in.readString(), in.readInt());

        long typeBits = in.readLong();
        for (Credential.Type i : Credential.Type.values()) {
            if ((typeBits & (1L << i.ordinal())) != 0L) {
                storedTypes.add(i);
            }
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(alias);
        out.writeInt(uid);

        long typeBits = 0;
        for (Credential.Type i : storedTypes) {
            typeBits |= 1L << i.ordinal();
        }
        out.writeLong(typeBits);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Credential> CREATOR
            = new Parcelable.Creator<Credential>() {
        public Credential createFromParcel(Parcel in) {
            return new Credential(in);
        }

        public Credential[] newArray(int size) {
            return new Credential[size];
        }
    };

    public boolean isSystem() {
        return UserHandle.getAppId(uid) == Process.SYSTEM_UID;
    }

    public String getAlias() { return alias; }

    public EnumSet<Credential.Type> getStoredTypes() {
        return storedTypes;
    }
}
