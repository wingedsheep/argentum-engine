package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * In the Darkness Bind Them
 * {2}{U}{B}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II, III — Create a 3/3 black Wraith creature token with menace. The Ring tempts you.
 * IV — For each opponent, gain control of up to one target creature that player controls until
 * end of turn. Untap those creatures. They gain haste until end of turn. The Ring tempts you.
 *
 * Chapter IV uses Kaya, Spirits' Justice's one-per-opponent distribution: `dynamicMaxCount =
 * PlayerCount(EachOpponent)` + `differentControllers = true`, `optional` for the "up to". Each
 * chosen creature is stolen, untapped and given haste by [Effects.ForEachTarget]; the Ring then
 * tempts you once — also when no targets were chosen (ruling), but not if every chosen target
 * became illegal (the ability fizzles).
 */
private const val WRAITH_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/9/a/9afb58e8-f5a7-49f9-8287-77757bd3268c.jpg?1783916058"

val InTheDarknessBindThem = card("In the Darkness Bind Them") {
    manaCost = "{2}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II, III — Create a 3/3 black Wraith creature token with menace. The Ring tempts you.\n" +
        "IV — For each opponent, gain control of up to one target creature that player controls " +
        "until end of turn. Untap those creatures. They gain haste until end of turn. The Ring tempts you."

    for (chapter in 1..3) {
        sagaChapter(chapter) {
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Wraith"),
                keywords = setOf(Keyword.MENACE),
                imageUri = WRAITH_TOKEN_IMAGE,
            ) then Effects.TheRingTemptsYou()
        }
    }

    sagaChapter(4) {
        targets(
            TargetFilter.CreatureOpponentControls,
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(
            Effects.GainControl(EffectTarget.ContextTarget(0), Duration.EndOfTurn),
            Effects.Untap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
        ) then Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7f7413b-0a65-4338-90eb-4b4c5462c21c.jpg?1783916019"

        ruling(
            "2023-06-16",
            "While resolving In the Darkness Bind Them's first three chapter abilities, you can choose " +
                "the Wraith token you just created to be your Ring-bearer as the Ring tempts you.",
        )
        ruling(
            "2023-06-16",
            "If a creature targeted by In the Darkness Bind Them's final chapter ability changes " +
                "controllers before the ability resolves, that creature is no longer a legal target.",
        )
        ruling(
            "2023-06-16",
            "When In the Darkness Bind Them's final chapter ability triggers, you can choose to target " +
                "no creatures just so that the Ring tempts you. However, if you do choose at least one " +
                "target, and all of those targets are illegal at the time the ability tries to resolve, " +
                "the ability won't resolve and none of its effects will happen. The Ring won't tempt you.",
        )
    }
}
