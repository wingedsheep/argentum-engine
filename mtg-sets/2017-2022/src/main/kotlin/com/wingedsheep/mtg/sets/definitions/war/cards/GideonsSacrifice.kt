package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RedirectScope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gideon's Sacrifice — War of the Spark #14 (canonical printing)
 * {W}
 * Instant
 * Choose a creature or planeswalker you control. All damage that would be dealt this turn to you and
 * permanents you control is dealt to the chosen permanent instead (if it's still on the battlefield).
 *
 * The permanent is chosen on resolution without targeting. The shield lists you and filters the
 * permanents you control (read at damage time, so a permanent that arrives later is covered), never
 * used up, lasting the turn. Damage already headed for the chosen permanent stays there, and once it
 * has left the battlefield or is no longer a creature or planeswalker nothing is redirected (CR 614.9).
 * Two of these choosing different permanents ask you where damage that could go to either ends up.
 */
val GideonsSacrifice = card("Gideon's Sacrifice") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose a creature or planeswalker you control. All damage that would be dealt this turn " +
        "to you and permanents you control is dealt to the chosen permanent instead (if it's still on the battlefield)."

    spell {
        effect = Effects.Pipeline {
            val chosen = selectTarget(
                TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.youControl())),
                nonTargeting = true,
                prompt = "Choose a creature or planeswalker you control to be dealt the damage dealt to you and your permanents"
            )
            run(Effects.RedirectNextDamage(
                protectedTargets = listOf(EffectTarget.Controller),
                redirectTo = chosen.asTarget,
                scope = RedirectScope.CONTINUOUS,
                protectedFilter = GameObjectFilter.Permanent.youControl(),
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Chris Rallis"
        flavorText = "To Liliana's surprise, the cost of betraying Bolas did not fall on her."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/849c79ad-8bfc-4512-ab41-f213b6b285ab.jpg?1783933481"
        ruling("2019-05-03", "Gideon's Sacrifice has no effect on damage already dealt earlier in the turn.")
        ruling(
            "2019-05-03",
            "If you control no creatures or planeswalkers as Gideon's Sacrifice resolves, nothing happens. " +
                "The damage redirection effect won't be able to be applied to any damage events."
        )
        ruling(
            "2019-05-03",
            "If the chosen creature or planeswalker isn't on the battlefield or isn't a creature or planeswalker " +
                "at the time damage would be dealt, the damage won't be redirected."
        )
        ruling(
            "2019-05-03",
            "More damage can be redirected to the chosen creature than it has toughness or to the chosen " +
                "planeswalker than it has loyalty, as long as that damage is all dealt at once (like combat damage " +
                "is) or is all dealt while a single spell or ability is resolving."
        )
        ruling("2019-05-03", "Gideon's Sacrifice doesn't change the source of the damage or whether the damage is combat damage.")
        ruling(
            "2019-05-03",
            "If you cast more than one Gideon's Sacrifice in one turn, all damage that would be dealt at once to " +
                "you and/or permanents you control is dealt to one of the chosen permanents of your choice. It's not " +
                "dealt to all of them, and you can't split the damage between them. The next time damage would be " +
                "dealt to you, you may choose a different one of those permanents to be dealt that damage."
        )
    }
}
