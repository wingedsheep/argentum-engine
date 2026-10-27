package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shackles of Treachery
 * {2}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. Until end of turn, it
 * gains haste and "Whenever this creature deals damage, destroy target Equipment attached to it."
 *
 * The granted trigger lives on the stolen creature, so its source is that creature and
 * `.attachedToSource()` scopes the target to Equipment attached to it — whoever controls the
 * Equipment. The target is re-checked on resolution, so an Equipment moved off the creature in
 * the meantime is no longer legal and survives. Combat damage dealt to several recipients at once
 * (trample, multiple blockers) is one event and triggers once.
 */
val ShacklesOfTreachery = card("Shackles of Treachery") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. Until end of turn, " +
        "it gains haste and \"Whenever this creature deals damage, destroy target Equipment attached to it.\""

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.GrantTriggeredAbility(
                ability = grantedTriggeredAbility {
                    trigger = Triggers.self.dealsDamage()
                    val equipment = target(
                        TargetFilter(GameObjectFilter.Artifact.withSubtype("Equipment").attachedToSource())
                    )
                    effect = Effects.Destroy(equipment)
                    description = "Whenever this creature deals damage, destroy target Equipment attached to it."
                },
                target = t
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Dmitry Burmak"
        flavorText = "In that instant, Toralf felt only the power of the storm and the need to destroy."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db743c6e-1385-468e-921a-7a525b88a1b6.jpg?1783928223"

        ruling(
            "2021-02-05",
            "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it."
        )
        ruling(
            "2021-02-05",
            "The triggered ability gained by the creature will trigger when that creature deals any damage, not just combat damage."
        )
        ruling(
            "2021-02-05",
            "If that ability triggers, you must choose a legal target if one is available. It doesn't matter who " +
                "controls the Equipment attached to the creature. If that Equipment is no longer attached to the " +
                "creature as the ability tries to resolve, the ability won't resolve and the Equipment won't be destroyed."
        )
    }
}
