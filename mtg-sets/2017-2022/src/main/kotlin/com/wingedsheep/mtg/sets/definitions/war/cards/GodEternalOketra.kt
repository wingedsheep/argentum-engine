package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * God-Eternal Oketra — War of the Spark #16 (canonical printing)
 * {3}{W}{W} · Legendary Creature — Zombie God · 3/6
 *
 * Double strike
 * Whenever you cast a creature spell, create a 4/4 black Zombie Warrior creature token with vigilance.
 * When God-Eternal Oketra dies or is put into exile from the battlefield, you may put it into its
 * owner's library third from the top.
 *
 * The cast trigger is the Skittering Skirge shape (`Triggers.you.casts(Creature)`), so it resolves
 * before the creature spell and even if that spell is countered. The last ability is the God-Eternal
 * shape shared with Bontu, Kefnet and Rhonas: one trigger per destination, `positionFromTop = 2`.
 */
val GodEternalOketra = card("God-Eternal Oketra") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Zombie God"
    power = 3
    toughness = 6
    oracleText = "Double strike\n" +
        "Whenever you cast a creature spell, create a 4/4 black Zombie Warrior creature token with vigilance.\n" +
        "When God-Eternal Oketra dies or is put into exile from the battlefield, you may put it into its " +
        "owner's library third from the top."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Warrior"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2e06aa97-1611-4af6-8bad-32051927fcfd.jpg?1783933349",
        )
        description = "Whenever you cast a creature spell, create a 4/4 black Zombie Warrior creature token " +
            "with vigilance."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD, positionFromTop = 2),
            prompt = "Put God-Eternal Oketra into its owner's library third from the top?"
        )
        description = "When God-Eternal Oketra dies, you may put it into its owner's library third from the top."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(to = Zone.EXILE)
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.EXILE, positionFromTop = 2),
            prompt = "Put God-Eternal Oketra into its owner's library third from the top?"
        )
        description = "When God-Eternal Oketra is put into exile from the battlefield, you may put it into its " +
            "owner's library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "16"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df70f155-2336-421c-8a9d-69772d2b51a8.jpg?1783933481"

        ruling(
            "2019-05-03",
            "Oketra's first triggered ability resolves before the spell that caused it to trigger. " +
                "It resolves even if that spell is countered."
        )
        ruling(
            "2019-05-03",
            "If the God's owner has two or fewer cards in their library, the God is put on the bottom of " +
                "their library as its last ability resolves."
        )
    }
}
