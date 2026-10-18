package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kami of Industry — Kamigawa: Neon Dynasty #149 (canonical printing)
 * {4}{R} · Creature — Spirit · 3/6
 *
 * When this creature enters, return target artifact card with mana value 3 or less from your
 * graveyard to the battlefield. It gains haste. Sacrifice it at the beginning of the next end step.
 *
 * The Cauldron Dance shape: the returned card is tracked, and haste plus the next-end-step sacrifice
 * are bound to it only if it actually arrived — a target that left the graveyard in response is not
 * chased. "Sacrifice it" is yours to do, so it's skipped if you no longer control it by then.
 */
val KamiOfIndustry = card("Kami of Industry") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 6
    oracleText = "When this creature enters, return target artifact card with mana value 3 or less " +
        "from your graveyard to the battlefield. It gains haste. Sacrifice it at the beginning of the " +
        "next end step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter(GameObjectFilter.Artifact.manaValueAtMost(3).ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val returned = gather(CardSource.ChosenTargets)
            move(returned, CardDestination.ToZone(Zone.BATTLEFIELD))
            ifNotEmpty(returned) {
                run(
                    Effects.GrantKeyword(
                        keyword = Keyword.HASTE,
                        target = returned.asTarget,
                        duration = Duration.Permanent
                    ) then
                        Effects.CreateDelayedTrigger(
                            step = Step.END,
                            effect = Effects.SacrificeTarget(returned.asTarget)
                        )
                )
            }
        }
        description = "When this creature enters, return target artifact card with mana value 3 or less " +
            "from your graveyard to the battlefield. It gains haste. Sacrifice it at the beginning of the " +
            "next end step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Chris Rahn"
        flavorText = "It takes many arms to arm an army."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3d00e61-3007-43da-960b-823e9ead2acf.jpg?1783923865"
    }
}
