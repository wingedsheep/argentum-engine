package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * God-Eternal Bontu — War of the Spark #92 (canonical printing)
 * {3}{B}{B} · Legendary Creature — Zombie God · 5/6
 *
 * Menace
 * When God-Eternal Bontu enters, sacrifice any number of other permanents, then draw that many
 * cards.
 * When God-Eternal Bontu dies or is put into exile from the battlefield, you may put her into her
 * owner's library third from the top.
 *
 * The enters trigger is Scapeshift's shape: [Effects.SacrificeAnyNumber] (other permanents, so
 * `excludeSource`) records what it sacrificed, and the draw reads
 * [DynamicAmounts.permanentsSacrificedThisWay] — choosing zero draws zero. "Dies or is put into
 * exile" is two triggers, one per destination (the Kaya the Inexorable shape); each moves the card
 * from the zone it went to, so a Bontu that has already left that zone stays where it is.
 * `positionFromTop = 2` is third from the top (0-indexed).
 */
val GodEternalBontu = card("God-Eternal Bontu") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie God"
    power = 5
    toughness = 6
    oracleText = "Menace\n" +
        "When God-Eternal Bontu enters, sacrifice any number of other permanents, then draw that many cards.\n" +
        "When God-Eternal Bontu dies or is put into exile from the battlefield, you may put her into her " +
        "owner's library third from the top."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.SacrificeAnyNumber(GameObjectFilter.Permanent, excludeSource = true) then
            Effects.DrawCards(DynamicAmounts.permanentsSacrificedThisWay())
        description = "When God-Eternal Bontu enters, sacrifice any number of other permanents, then draw that many cards."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.GRAVEYARD, positionFromTop = 2),
            prompt = "Put God-Eternal Bontu into its owner's library third from the top?"
        )
        description = "When God-Eternal Bontu dies, you may put her into her owner's library third from the top."
    }

    triggeredAbility {
        trigger = Triggers.self.leaves(to = Zone.EXILE)
        effect = Effects.May(
            Effects.Move(EffectTarget.Self, Zone.LIBRARY, fromZone = Zone.EXILE, positionFromTop = 2),
            prompt = "Put God-Eternal Bontu into its owner's library third from the top?"
        )
        description = "When God-Eternal Bontu is put into exile from the battlefield, you may put her into her " +
            "owner's library third from the top."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "92"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3714a135-e2b9-43a7-a2a2-fa5a2e0ac61a.jpg?1783933445"

        ruling(
            "2019-05-03",
            "If any abilities trigger as you sacrifice other permanents, those abilities won't be put onto the " +
                "stack until after you've drawn cards."
        )
        ruling(
            "2019-05-03",
            "As Bontu's first triggered ability resolves, you may choose to sacrifice zero other permanents."
        )
        ruling(
            "2019-05-03",
            "If a second God-Eternal Bontu enters the battlefield under your control, you'll put one into your " +
                "graveyard before you can sacrifice anything to the new Bontu's triggered ability."
        )
    }
}
