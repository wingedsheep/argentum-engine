package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Balduvian Atrocity {2}{B}
 * Creature — Phyrexian Berserker 2/3
 *
 * The kicked ETB returns the targeted card; haste and the next-end-step sacrifice are bound to the
 * tracked permanent that actually arrived, so a card that left the graveyard first is not chased.
 */
val BalduvianAtrocity = card("Balduvian Atrocity") {
    manaCost = "{2}{B}"
    colorIdentity = "BR"
    typeLine = "Creature — Phyrexian Berserker"
    power = 2
    toughness = 3
    oracleText = "Kicker {R} (You may pay an additional {R} as you cast this spell.)\nMenace\n" +
        "When this creature enters, if it was kicked, return target creature card with mana value 3 or " +
        "less from your graveyard to the battlefield. It gains haste. Sacrifice it at the beginning of " +
        "the next end step."

    keywordAbility(KeywordAbility.kicker("{R}"))
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        target(
            TargetFilter(GameObjectFilter.Creature.manaValueAtMost(3).ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Pipeline {
            val entered = moveTracked(
                gather(CardSource.ChosenTargets),
                CardDestination.ToZone(Zone.BATTLEFIELD),
            )
            ifNotEmpty(entered) {
                run(Effects.GrantKeyword(
                    keyword = Keyword.HASTE,
                    target = entered.asTarget,
                    duration = Duration.Permanent,
                ))
                run(Effects.CreateDelayedTrigger(
                    step = Step.END,
                    effect = Effects.SacrificeTarget(entered.asTarget),
                ))
            }
        }
        description = "Return target creature card with mana value 3 or less from your graveyard to the " +
            "battlefield. It gains haste. Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Mike Jordana"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/365333c4-59a0-4365-b242-a67b6224c1b8.jpg?1783921339"
    }
}
