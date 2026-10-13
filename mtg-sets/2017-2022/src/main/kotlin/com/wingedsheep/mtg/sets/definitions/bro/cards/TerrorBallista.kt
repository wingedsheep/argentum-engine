package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Terror Ballista — The Brothers' War #290 (non-booster extra)
 * {7} · Artifact Creature — Construct · Rare
 * 5/3
 *
 * Menace
 * Whenever this creature attacks, you may sacrifice another creature. When you do, destroy
 * target creature an opponent controls.
 * Unearth {3}{B}{B}
 *
 * The attack trigger is Ruthless Lawbringer's shape: the sacrifice is a resolution-time,
 * non-targeting choice, and only once it happens does the reflexive trigger choose its target.
 */
val TerrorBallista = card("Terror Ballista") {
    manaCost = "{7}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Construct"
    power = 5
    toughness = 3
    oracleText = "Menace\n" +
        "Whenever this creature attacks, you may sacrifice another creature. When you do, destroy target creature an opponent controls.\n" +
        "Unearth {3}{B}{B} ({3}{B}{B}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. " +
        "Unearth only as a sorcery.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val creatureToSacrifice = selectTarget(
                    TargetObject(
                        filter = TargetFilter.CreatureYouControl.other()
                    ),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                run(Effects.SacrificeTarget(creatureToSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, destroy target creature an opponent controls."
        ) {
            val opposingCreature = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.Destroy(opposingCreature)
        }
        description = "Whenever this creature attacks, you may sacrifice another creature. When you do, destroy target creature an opponent controls."
    }

    unearth("{3}{B}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "290"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31215674-0bcd-46f1-b8a1-5415f8b674d1.jpg?1783919994"
        ruling("2022-10-14", "You don't choose a target for Terror Ballista's triggered ability when it attacks. If you choose to sacrifice a creature, a second triggered ability triggers and you choose a target for that second ability. Players may respond as normal to this second ability.")
    }
}
