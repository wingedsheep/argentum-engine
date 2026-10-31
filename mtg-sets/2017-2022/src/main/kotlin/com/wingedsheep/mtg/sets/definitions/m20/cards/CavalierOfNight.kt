package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cavalier of Night — Core Set 2020 #94
 * {2}{B}{B}{B} · Creature — Elemental Knight 4/5
 *
 * Lifelink
 * When this creature enters, you may sacrifice another creature. When you do, destroy target
 * creature an opponent controls.
 * When this creature dies, return target creature card with mana value 3 or less from your
 * graveyard to the battlefield.
 *
 * The sacrifice is a resolution-time choice (not a target); the reflexive "when you do" trigger
 * picks its target only after the sacrifice, as on Ruthless Lawbringer.
 */
val CavalierOfNight = card("Cavalier of Night") {
    manaCost = "{2}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elemental Knight"
    power = 4
    toughness = 5
    oracleText = "Lifelink\n" +
        "When this creature enters, you may sacrifice another creature. When you do, destroy target " +
        "creature an opponent controls.\n" +
        "When this creature dies, return target creature card with mana value 3 or less from your " +
        "graveyard to the battlefield."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val creatureToSacrifice = selectTarget(
                    TargetObject(filter = TargetFilter.CreatureYouControl.other()),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                run(Effects.SacrificeTarget(creatureToSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, destroy target creature " +
                "an opponent controls."
        ) {
            val creature = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.Destroy(creature)
        }
        description = "When this creature enters, you may sacrifice another creature. When you do, destroy " +
            "target creature an opponent controls."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3))
        effect = Effects.Move(card, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f03109b9-54a8-4d68-97c2-e42c7d5c2d41.jpg?1783932996"
        ruling(
            "2019-07-12",
            "Cavalier of Night's enters-the-battlefield ability goes on the stack without a target. While that " +
                "ability is resolving, you may sacrifice another creature. When you do, the reflexive triggered " +
                "ability triggers and you pick a target creature to be destroyed."
        )
        ruling(
            "2019-07-12",
            "While resolving Cavalier of Night's enters-the-battlefield ability, you can't sacrifice more than " +
                "one creature."
        )
        ruling(
            "2019-07-12",
            "If a creature card with mana value 3 or less becomes a copy of Cavalier of Night, its last ability " +
                "can target itself when it dies."
        )
    }
}
