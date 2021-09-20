
export interface PendingUser {
    kind: "PendingUser"
}

export interface SignedOutUser {
    kind: "SignedOutUser"
}

export interface SignedInUser {
    kind: "SignedInUser"
    profile: UserProfile
}

export type UserProfile = {
    email: string;
    ports: string[];
    roles: string[];
}

export type User = PendingUser | SignedOutUser | SignedInUser
