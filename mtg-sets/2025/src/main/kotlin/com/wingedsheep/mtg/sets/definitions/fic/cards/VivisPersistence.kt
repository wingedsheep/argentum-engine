package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vivi's Persistence — Final Fantasy Commander #458
 * {1}{R} · Instant
 * Create a 0/1 black Wizard creature token with "Whenever you cast a noncreature spell, this
 * token deals 1 damage to each opponent."
 * Whenever your commander enters or attacks, you may pay {2}. If you do, return this card from
 * your graveyard to your hand.
 *
 * The recursion trigger moves the card out of the graveyard, so it functions only there
 * (CR 113.6m). "Your commander" is the card designated as the commander of the deck you own
 * (CR 903.3), hence `commander().ownedByYou()`; each of your attacking commanders is its own
 * occurrence of the trigger event (CR 603.2c).
 */
val VivisPersistence = card("Vivi's Persistence") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Create a 0/1 black Wizard creature token with \"Whenever you cast a noncreature spell, " +
        "this token deals 1 damage to each opponent.\"\nWhenever your commander enters or attacks, you may " +
        "pay {2}. If you do, return this card from your graveyard to your hand."

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Wizard"),
            imageUri = "https://cards.scryfall.io/normal/front/1/8/187fe54c-7d0c-4225-9d46-3affbead897d.jpg?1782725378",
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                    effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                ),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.a(GameObjectFilter.Any.commander().ownedByYou()).enters(),
            Triggers.a(GameObjectFilter.Any.commander().ownedByYou()).attacks(),
        )
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "458"
        artist = "Erion Makuo"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be6ba2e4-e657-4a2d-8f5f-255376d861b3.jpg?1783904652"

        ruling("2025-10-02", "The Wizard token's ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
