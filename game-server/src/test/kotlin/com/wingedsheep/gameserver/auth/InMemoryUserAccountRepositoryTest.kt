package com.wingedsheep.gameserver.auth

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class InMemoryUserAccountRepositoryTest : FunSpec({

    test("save then findById round-trips an account") {
        val repo = InMemoryUserAccountRepository()
        val account = newAccount("Alice", 1.toShort())

        repo.save(account)

        repo.findById(account.id) shouldBe account
    }

    test("findById returns null for an unknown id") {
        InMemoryUserAccountRepository().findById(UUID.randomUUID()).shouldBeNull()
    }

    test("findByProviderAndSubject locates by the natural key") {
        val repo = InMemoryUserAccountRepository()
        val google = newAccount("Alice", 1.toShort(), provider = "google", subject = "g-123")
        val github = newAccount("Alice", 2.toShort(), provider = "github", subject = "gh-456")
        repo.save(google)
        repo.save(github)

        repo.findByProviderAndSubject("google", "g-123") shouldBe google
        repo.findByProviderAndSubject("github", "gh-456") shouldBe github
        repo.findByProviderAndSubject("google", "missing").shouldBeNull()
    }

    test("findByDisplayNameIgnoreCaseAndDiscriminator matches case-insensitively") {
        val repo = InMemoryUserAccountRepository()
        val alice = newAccount("Alice", 42.toShort())
        repo.save(alice)

        repo.findByDisplayNameIgnoreCaseAndDiscriminator("alice", 42.toShort()) shouldBe alice
        repo.findByDisplayNameIgnoreCaseAndDiscriminator("ALICE", 42.toShort()) shouldBe alice
        repo.findByDisplayNameIgnoreCaseAndDiscriminator("Alice", 99.toShort()).shouldBeNull()
    }

    test("findDiscriminatorsInUseFor collects all discriminators for a name (any case)") {
        val repo = InMemoryUserAccountRepository()
        repo.save(newAccount("Alice", 1.toShort()))
        repo.save(newAccount("alice", 7.toShort()))
        repo.save(newAccount("ALICE", 42.toShort()))
        repo.save(newAccount("Bob", 1.toShort()))

        repo.findDiscriminatorsInUseFor("Alice")
            .shouldContainExactlyInAnyOrder(1.toShort(), 7.toShort(), 42.toShort())
        repo.findDiscriminatorsInUseFor("alice")
            .shouldContainExactlyInAnyOrder(1.toShort(), 7.toShort(), 42.toShort())
        repo.findDiscriminatorsInUseFor("Bob")
            .shouldContainExactlyInAnyOrder(1.toShort())
        repo.findDiscriminatorsInUseFor("Charlie") shouldBe emptySet()
    }

    test("save with the same id overwrites the existing row") {
        val repo = InMemoryUserAccountRepository()
        val id = UUID.randomUUID()
        val original = newAccount("Alice", 1.toShort(), id = id)
        repo.save(original)

        val renamed = newAccount("Bob", 1.toShort(), id = id)
        repo.save(renamed)

        repo.findById(id)?.displayName shouldBe "Bob"
    }
})

private fun newAccount(
    displayName: String,
    discriminator: Short,
    id: UUID = UUID.randomUUID(),
    provider: String = "google",
    subject: String = UUID.randomUUID().toString()
): UserAccount = UserAccount(
    id = id,
    provider = provider,
    subject = subject,
    email = "$displayName@example.com",
    displayName = displayName,
    discriminator = discriminator,
    avatarUrl = null,
    createdAt = Instant.now(),
    lastLoginAt = Instant.now()
)
