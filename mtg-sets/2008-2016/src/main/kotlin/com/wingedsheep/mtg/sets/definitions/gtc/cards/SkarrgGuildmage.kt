package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skarrg Guildmage — Gatecrash #196
 * {R}{G} · Creature — Human Shaman · 2 / 2
 *
 * {R}{G}: Creatures you control gain trample until end of turn.
 * {1}{R}{G}: Target land you control becomes a 4/4 Elemental creature until end of turn.
 * It's still a land.
 */
val SkarrgGuildmage = card("Skarrg Guildmage") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "{R}{G}: Creatures you control gain trample until end of turn.\n" +
        "{1}{R}{G}: Target land you control becomes a 4/4 Elemental creature until end of turn. It's still a land."

    activatedAbility {
        cost = Costs.Mana("{R}{G}")
        effect = Patterns.Group.grantKeywordToAll(Keyword.TRAMPLE, Filters.Group.creaturesYouControl)
    }
    activatedAbility {
        cost = Costs.Mana("{1}{R}{G}")
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.BecomeCreature(
            target = land,
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "196"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00f206c0-a8a7-4ca0-b88f-4736c3dac588.jpg"
    }
}
