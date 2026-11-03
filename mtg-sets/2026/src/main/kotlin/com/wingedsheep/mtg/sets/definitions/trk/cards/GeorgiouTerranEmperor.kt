package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCantLoseGameFromLife
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Georgiou, Terran Emperor
 * {4}{B}{B}
 * Legendary Creature — Human Rogue Noble
 * 4/6
 * You don't lose the game for having 0 or less life.
 * At the beginning of your end step, you lose 3 life and draw a card. Then if your life total is
 * 0 or less, create ten 1/1 red Officer creature tokens.
 *
 * The first line is the narrow [GrantCantLoseGameFromLife] static (Marina Vendrell's Grimoire),
 * not Platinum Angel's broad one. The "then if" is checked on resolution after the life loss.
 */
val GeorgiouTerranEmperor = card("Georgiou, Terran Emperor") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Rogue Noble"
    power = 4
    toughness = 6
    oracleText = "You don't lose the game for having 0 or less life.\n" +
        "At the beginning of your end step, you lose 3 life and draw a card. Then if your life total is 0 or less, create ten 1/1 red Officer creature tokens."

    // You don't lose the game for having 0 or less life.
    staticAbility {
        ability = GrantCantLoseGameFromLife
    }

    // At the beginning of your end step, you lose 3 life and draw a card. Then if your life total
    // is 0 or less, create ten 1/1 red Officer creature tokens.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.LoseLife(3, EffectTarget.Controller) then
            Effects.DrawCards(1) then
            Effects.If(
                condition = Conditions.LifeAtMost(0),
                then = Effects.CreateToken(
                    power = 1, toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Officer"),
                    count = 10,
                    imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
                )
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "110"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a68361a3-83fa-44b5-92b5-b7905866fc99.jpg?1792753873"
    }
}
