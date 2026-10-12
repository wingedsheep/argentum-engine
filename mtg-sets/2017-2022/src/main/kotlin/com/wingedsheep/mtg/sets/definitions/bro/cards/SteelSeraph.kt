package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Steel Seraph
 * {6}
 * Artifact Creature — Angel
 * 5/4
 * Prototype {1}{W}{W} — 3/3
 * Flying
 * At the beginning of combat on your turn, target creature you control gains your choice of flying,
 * vigilance, or lifelink until end of turn.
 *
 * "Your choice of" is not a printed "Choose one —", so the keyword is picked **on resolution** (the
 * card's ruling), not as the trigger goes on the stack — [Effects.ChooseAction], as Razor Barrier
 * does, rather than a modal effect. The target is still chosen when the trigger is put on the stack.
 */
val SteelSeraph = card("Steel Seraph") {
    manaCost = "{6}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Angel"
    power = 5
    toughness = 4
    oracleText = "Prototype {1}{W}{W} — 3/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "Flying\n" +
        "At the beginning of combat on your turn, target creature you control gains your choice of flying, " +
        "vigilance, or lifelink until end of turn."

    keywordAbility(KeywordAbility.prototype("{1}{W}{W}", 3, 3))

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "Flying",
                    effect = Effects.GrantKeyword(Keyword.FLYING, creature, Duration.EndOfTurn)
                ),
                EffectChoice(
                    label = "Vigilance",
                    effect = Effects.GrantKeyword(Keyword.VIGILANCE, creature, Duration.EndOfTurn)
                ),
                EffectChoice(
                    label = "Lifelink",
                    effect = Effects.GrantKeyword(Keyword.LIFELINK, creature, Duration.EndOfTurn)
                )
            )
        )
        description = "At the beginning of combat on your turn, target creature you control gains your " +
            "choice of flying, vigilance, or lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Denys Tsiperko"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b6ef5f5-4058-4f89-a573-9e2da87a9f2e.jpg?1783920118"
        ruling("2022-10-14", "You choose the target creature as you put Steel Seraph's triggered ability on the stack. You choose which ability that creature gains as the ability resolves.")
    }
}
