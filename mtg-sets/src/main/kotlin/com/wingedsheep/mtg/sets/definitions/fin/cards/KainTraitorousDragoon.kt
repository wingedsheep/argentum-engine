package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kain, Traitorous Dragoon
 * {2}{B}
 * Legendary Creature — Human Knight
 * 2/4
 *
 * Jump — During your turn, Kain has flying.
 * Whenever Kain deals combat damage to a player, that player gains control of Kain. If they do, you
 * draw that many cards, create that many tapped Treasure tokens, then lose that much life.
 *
 * "Jump" is an ability word (no rules meaning); it's modeled as a conditional static grant of flying
 * to this creature gated on [Conditions.IsYourTurn] — the same shape as Freya Crescent / Dragoon's
 * Lance.
 *
 * The combat-damage rider reuses the [SuccessCriterion.ControlChanged] idiom (Stiltzkin, Moogle
 * Merchant): the donation of Kain to the damaged player is the gated action, and the draw / Treasure
 * / life-loss payoff fires only when control actually moves. If Kain has already left the battlefield
 * by the time the ability resolves, no control changes and the "if they do" rider is withheld, per the
 * printed wording. "That many / that much" all read the triggering combat-damage amount
 * ([ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT]); "you" stays the ability's controller (Kain's controller
 * when it dealt the damage), so the draw, Treasures, and life loss all belong to the original
 * controller even after control of Kain has changed.
 */
val KainTraitorousDragoon = card("Kain, Traitorous Dragoon") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Jump — During your turn, Kain has flying.\n" +
        "Whenever Kain deals combat damage to a player, that player gains control of Kain. If they " +
        "do, you draw that many cards, create that many tapped Treasure tokens, then lose that much life."
    power = 2
    toughness = 4

    // Jump — During your turn, Kain has flying.
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.FLYING, Filters.Self)
    }

    // Whenever Kain deals combat damage to a player, that player gains control of Kain. If they do,
    // you draw that many cards, create that many tapped Treasure tokens, then lose that much life.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        val damage = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
        effect = IfYouDoEffect(
            action = GiveControlToTargetPlayerEffect(
                permanent = EffectTarget.Self,
                newController = EffectTarget.PlayerRef(Player.DefendingPlayer),
            ),
            ifYouDo = Effects.Composite(
                listOf(
                    Effects.DrawCards(damage, EffectTarget.Controller),
                    Effects.CreateTreasure(damage, tapped = true),
                    Effects.LoseLife(damage, EffectTarget.Controller),
                )
            ),
            successCriterion = SuccessCriterion.ControlChanged,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Russell Dongjun Lu"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8c86be0-e1b3-4a78-9254-238dd936914b.jpg?1782686519"
    }
}
