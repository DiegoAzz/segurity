package com.norton.parental.companion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable representation of the parental control policy applicable to this device.
 */
public final class PolicyModel {

    private final long policyVersion;
    private final boolean instantLockActive;
    private final Set<String> blockedPackages;
    private final Set<String> blockedDomains;
    private final Set<String> blockedKeywords;
    private final boolean adultBlocked;
    private final int dailyLimitMinutes;
    private final int usedTodayMinutes;
    private final boolean bedtimeActive;
    private final boolean schoolTimeActive;
    private final long updatedAt;

    public PolicyModel(long policyVersion,
                       boolean instantLockActive,
                       Set<String> blockedPackages,
                       Set<String> blockedDomains,
                       Set<String> blockedKeywords,
                       boolean adultBlocked,
                       int dailyLimitMinutes,
                       int usedTodayMinutes,
                       boolean bedtimeActive,
                       boolean schoolTimeActive,
                       long updatedAt) {
        this.policyVersion = policyVersion;
        this.instantLockActive = instantLockActive;
        this.blockedPackages = blockedPackages != null ? Collections.unmodifiableSet(new HashSet<>(blockedPackages)) : Collections.emptySet();
        this.blockedDomains = blockedDomains != null ? Collections.unmodifiableSet(new HashSet<>(blockedDomains)) : Collections.emptySet();
        this.blockedKeywords = blockedKeywords != null ? Collections.unmodifiableSet(new HashSet<>(blockedKeywords)) : Collections.emptySet();
        this.adultBlocked = adultBlocked;
        this.dailyLimitMinutes = dailyLimitMinutes;
        this.usedTodayMinutes = usedTodayMinutes;
        this.bedtimeActive = bedtimeActive;
        this.schoolTimeActive = schoolTimeActive;
        this.updatedAt = updatedAt;
    }

    public static PolicyModel createDefault() {
        return new PolicyModel(
                1L,
                false,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                true,
                120,
                0,
                false,
                false,
                System.currentTimeMillis()
        );
    }

    public long getPolicyVersion() {
        return policyVersion;
    }

    public boolean isInstantLockActive() {
        return instantLockActive;
    }

    public Set<String> getBlockedPackages() {
        return blockedPackages;
    }

    public boolean isPackageBlocked(String pkg) {
        return pkg != null && blockedPackages.contains(pkg);
    }

    public Set<String> getBlockedDomains() {
        return blockedDomains;
    }

    public Set<String> getBlockedKeywords() {
        return blockedKeywords;
    }

    public boolean isAdultBlocked() {
        return adultBlocked;
    }

    public int getDailyLimitMinutes() {
        return dailyLimitMinutes;
    }

    public int getUsedTodayMinutes() {
        return usedTodayMinutes;
    }

    public boolean isBedtimeActive() {
        return bedtimeActive;
    }

    public boolean isSchoolTimeActive() {
        return schoolTimeActive;
    }

    public boolean isTimeQuotaExhausted() {
        return dailyLimitMinutes > 0 && usedTodayMinutes >= dailyLimitMinutes;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "PolicyModel{" +
                "v=" + policyVersion +
                ", instantLock=" + instantLockActive +
                ", blockedApps=" + blockedPackages.size() +
                ", adult=" + adultBlocked +
                ", quotaExhausted=" + isTimeQuotaExhausted() +
                '}';
    }
}
