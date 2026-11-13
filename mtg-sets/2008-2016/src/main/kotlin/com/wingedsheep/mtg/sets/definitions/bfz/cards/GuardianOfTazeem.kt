package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian of Tazeem — Battle for Zendikar #78
 * {3}{U}{U} · Creature — Sphinx · 4/5
 * Flying
 * Landfall — Whenever a land you control enters, tap target creature an opponent controls. If that
 * land is an Island, that creature doesn't untap during its controller's next untap step.
 *
 * "If that land is an Island" reads the triggering land on resolution — Emeria Shepherd's
 * `EntityMatches(TriggeringEntity, …)` shape. The freeze is Adverse Conditions' shape: the target
 * gets [AbilityFlag.DOESNT_UNTAP] until after its *current* controller's next untap step, so it
 * tracks the creature rather than the controller, as the ruling requires. An already-tapped
 * creature is still a legal target and is still frozen.
 */
val GuardianOfTazeem = card("Guardian of Tazeem") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Landfall — Whenever a land you control enters, tap target creature an opponent controls. " +
        "If that land is an Island, that creature doesn't untap during its controller's next untap step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Land.withSubtype("Island"),
            ),
            then = Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap()
            ),
        )
        description = "Landfall — Whenever a land you control enters, tap target creature an opponent " +
            "controls. If that land is an Island, that creature doesn't untap during its controller's " +
            "next untap step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b55cf5e-4499-4e3a-9748-7b1568286b5a.jpg?1783938208"
        ruling(
            "2015-08-25",
            "Guardian of Tazeem's landfall ability can target a creature that's already tapped. If the " +
                "land that caused it to trigger was an Island, that creature won't untap during its " +
                "controller's next untap step."
        )
        ruling(
            "2015-08-25",
            "The ability tracks the creature, but not its controller. If the creature changes controllers " +
                "before its first controller's next untap step has come around, then it won't untap during " +
                "its new controller's next untap step."
        )
    }
}
