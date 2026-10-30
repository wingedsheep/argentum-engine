package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hei Bai, Forest Guardian
 * {3}{G}
 * Legendary Creature — Bear Spirit
 * 4/4
 *
 * When Hei Bai enters, reveal cards from the top of your library until you reveal a Shrine card.
 * You may put that card onto the battlefield. Then shuffle.
 * {W}{U}{B}{R}{G}, {T}: For each legendary enchantment you control, create a 1/1 colorless Spirit
 * creature token with "This token can't block or be blocked by non-Spirit creatures."
 *
 * - The ETB is Thicket Elemental's reveal-until pipeline with the "you may" moved onto the found
 *   card: an up-to-one choice over the matched Shrine, so the reveal always happens and only the
 *   battlefield move is optional. Everything not moved is still in the library when it's shuffled.
 *   With no Shrine in the library the whole library is revealed and nothing is chosen.
 * - The token is Foggy Swamp Spirit Keeper's Spirit; the count of legendary enchantments you
 *   control is read as the ability resolves.
 */
val HeiBaiForestGuardian = card("Hei Bai, Forest Guardian") {
    manaCost = "{3}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Creature — Bear Spirit"
    power = 4
    toughness = 4
    oracleText = "When Hei Bai enters, reveal cards from the top of your library until you reveal a Shrine card. " +
        "You may put that card onto the battlefield. Then shuffle.\n" +
        "{W}{U}{B}{R}{G}, {T}: For each legendary enchantment you control, create a 1/1 colorless Spirit " +
        "creature token with \"This token can't block or be blocked by non-Spirit creatures.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val (shrine, allRevealed) = gatherUntilMatch(GameObjectFilter.Any.withSubtype("Shrine"))
            reveal(allRevealed)
            val chosen = chooseUpTo(
                1,
                from = shrine,
                prompt = "You may put the revealed Shrine card onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                alwaysPrompt = true,
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD))
            run(Effects.ShuffleLibrary())
        }
        description = "When Hei Bai enters, reveal cards from the top of your library until you reveal a " +
            "Shrine card. You may put that card onto the battlefield. Then shuffle."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}{U}{B}{R}{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Enchantment.legendary()).count(),
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Spirit"),
            name = "Spirit",
            imageUri = "https://cards.scryfall.io/normal/front/f/5/f59eba51-458a-40e0-b754-999f91d5d839.jpg?1783904748",
            staticAbilities = listOf(
                CantBeBlockedExceptBy(
                    blockerFilter = GameObjectFilter.Creature.withSubtype("Spirit")
                ),
                CanOnlyBlockCreaturesWith(
                    blockerFilter = GameObjectFilter.Creature.withSubtype("Spirit")
                )
            )
        )
        description = "{W}{U}{B}{R}{G}, {T}: For each legendary enchantment you control, create a 1/1 colorless " +
            "Spirit creature token with \"This token can't block or be blocked by non-Spirit creatures.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "139"
        artist = "TAPIOCA"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95a3fda4-64d6-4440-a77c-b249edca7860.jpg?1783904812"
    }
}
