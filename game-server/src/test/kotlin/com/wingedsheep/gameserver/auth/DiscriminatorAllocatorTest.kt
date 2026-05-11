package com.wingedsheep.gameserver.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class DiscriminatorAllocatorTest : FunSpec({

    test("first allocation for a fresh display name returns 1") {
        val repo = InMemoryUserAccountRepository()
        val allocator = DiscriminatorAllocator(repo)

        allocator.allocate("Alice") shouldBe 1.toShort()
    }

    test("subsequent allocations fill 1..N as accounts are saved") {
        val repo = InMemoryUserAccountRepository()
        val allocator = DiscriminatorAllocator(repo)

        repo.save(account("Alice", 1.toShort()))
        repo.save(account("Alice", 2.toShort()))

        allocator.allocate("Alice") shouldBe 3.toShort()
    }

    test("allocator picks the lowest gap, not the next-after-max") {
        val repo = InMemoryUserAccountRepository()
        repo.save(account("Alice", 1.toShort()))
        repo.save(account("Alice", 3.toShort()))   // gap at 2
        repo.save(account("Alice", 4.toShort()))

        DiscriminatorAllocator(repo).allocate("Alice") shouldBe 2.toShort()
    }

    test("display-name matching is case-insensitive — Alice and alice share a pool") {
        val repo = InMemoryUserAccountRepository()
        repo.save(account("Alice", 1.toShort()))
        repo.save(account("ALICE", 2.toShort()))
        repo.save(account("alice", 3.toShort()))

        DiscriminatorAllocator(repo).allocate("alice") shouldBe 4.toShort()
    }

    test("different display names get independent pools") {
        val repo = InMemoryUserAccountRepository()
        repo.save(account("Alice", 1.toShort()))

        // Bob's pool is untouched — Bob#0001 is still free.
        DiscriminatorAllocator(repo).allocate("Bob") shouldBe 1.toShort()
    }

    test("exhausted pool (all 9999 slots taken) throws DiscriminatorPoolExhaustedException") {
        // Pre-populate every slot. Done with a fake repo to avoid 9999 inserts in a test.
        val exhaustedRepo = object : UserAccountRepository by InMemoryUserAccountRepository() {
            override fun findDiscriminatorsInUseFor(displayName: String): Set<Short> =
                (1..9999).mapTo(mutableSetOf()) { it.toShort() }
        }

        shouldThrow<DiscriminatorPoolExhaustedException> {
            DiscriminatorAllocator(exhaustedRepo).allocate("Alice")
        }
    }

    test("handle is zero-padded to four digits") {
        val account = account("Alice", 42.toShort())
        account.handle shouldBe "Alice#0042"
    }

    test("handle preserves display-name casing exactly as stored") {
        account("aLiCe", 7.toShort()).handle shouldBe "aLiCe#0007"
    }
})

private fun account(displayName: String, discriminator: Short): UserAccount = UserAccount(
    id = UUID.randomUUID(),
    provider = "test",
    subject = UUID.randomUUID().toString(),
    email = "$displayName@example.com",
    displayName = displayName,
    discriminator = discriminator,
    avatarUrl = null,
    createdAt = Instant.now(),
    lastLoginAt = Instant.now()
)
