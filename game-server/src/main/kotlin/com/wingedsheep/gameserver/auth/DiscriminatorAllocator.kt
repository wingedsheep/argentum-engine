package com.wingedsheep.gameserver.auth

import org.springframework.stereotype.Service

/**
 * Picks the next unused discriminator for a given display name, producing Discord-style
 * `Alice#0042` handles. Display-name matching is case-insensitive — `Alice` and `alice`
 * share a discriminator pool.
 *
 * Concurrency: the read-then-allocate is **not** transactional, so two concurrent allocators
 * racing for the same display name could both pick the same number. The caller (typically
 * `AccountProvisioner` — slice 2) handles the resulting unique-constraint violation by retrying
 * via [allocate]; a small retry loop is sufficient for practical contention. If contention ever
 * becomes measurable we can replace the loop with a `pg_advisory_xact_lock(hashtext(...))`
 * around insert; not needed now.
 */
@Service
class DiscriminatorAllocator(
    private val repository: UserAccountRepository
) {

    /**
     * @throws DiscriminatorPoolExhaustedException if all 9999 slots for [displayName] are taken
     *         (practically impossible at foreseeable user counts; revisit if anyone hits it).
     */
    fun allocate(displayName: String): Short {
        val inUse = repository.findDiscriminatorsInUseFor(displayName)
        for (candidate in MIN_DISCRIMINATOR..MAX_DISCRIMINATOR) {
            val short = candidate.toShort()
            if (short !in inUse) return short
        }
        throw DiscriminatorPoolExhaustedException(displayName)
    }

    companion object {
        const val MIN_DISCRIMINATOR = 1
        const val MAX_DISCRIMINATOR = 9999
    }
}

class DiscriminatorPoolExhaustedException(displayName: String) :
    RuntimeException("All discriminators 1..9999 exhausted for displayName \"$displayName\"")
