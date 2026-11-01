package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * God-Eternal Rhonas — War of the Spark #163 (canonical printing)
 * {3}{G}{G} · Legendary Creature — Zombie God · 5/5
 *
 * Deathtouch
 * When God-Eternal Rhonas enters, double the power of each other creature you control until end of
 * turn. Those creatures gain vigilance until end of turn.
 * When God-Eternal Rhonas dies or is put into exile from the battlefield, you may put it into its
 * owner's library third from the top.
 *
 * The enters trigger gathers "each other creature you control" once and, per creature, gives it
 * +X/+0 (X = its power as the effect applies — the ruling's reading of "double") and vigilance, so
 * "those creatures" are exactly the ones doubled. The last ability is the God-Eternal shape shared
 * with Bontu and Kefnet: one trigger per destination, `positionFromTop = 2` (third from the top).
 */
val GodEternalRhonas = card("God-Eternal Rhonas") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Zombie God"
    power = 5
    toughness = 5
    oracleText = "Deathtouch\n" +
        "When God-Eternal Rhonas enters, double the power of each other creature you control until end of turn. " +
        "Those creatures gain vigilance until end of turn.\n" +
        "When God-Eternal Rhonas dies or is put into exile from the battlefield, you may put it into its " +
        "owner's library third from the top."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            Filters.Group.otherCreaturesYouControl,
            Effects.ModifyStats(
                power = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                toughness = DynamicAmounts.fixed(0),
                target = EffectTarget.IterationEntity
            ) then Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity)
        )
        description = "When God-Eternal Rhonas enters, double the power of each other creature you control until " +
            "end of turn. Those creatures gain vigilance until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD, positionFromTop = 2),
            prompt = "Put God-Eternal Rhonas into its owner's library third from the top?"
        )
        description = "When God-Eternal Rhonas dies, you may put it into its owner's library third from the top."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(to = Zone.EXILE)
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.EXILE, positionFromTop = 2),
            prompt = "Put God-Eternal Rhonas into its owner's library third from the top?"
        )
        description = "When God-Eternal Rhonas is put into exile from the battlefield, you may put it into its " +
            "owner's library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0eb444f0-69cc-47c8-8cd6-1f9edad5d903.jpg?1783933412"

        ruling(
            "2019-05-03",
            "Rhonas's first triggered ability affects only creatures you control at the time it resolves. " +
                "Creatures you begin to control later in the turn won't have their power doubled or gain vigilance."
        )
        ruling(
            "2019-05-03",
            "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is " +
                "its power as that effect begins to apply."
        )
    }
}
