package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Offspring's Revenge — Ikoria: Lair of Behemoths #198
 * {2}{R}{W}{B} · Enchantment
 *
 * At the beginning of combat on your turn, exile target red, white, or black creature card from
 * your graveyard. Create a token that's a copy of that card, except it's 1/1. It gains haste
 * until your next turn.
 *
 * Soul Separator's exile-then-copy shape: the target handle still names the card after it moves to
 * exile, and the token copies its printed characteristics with a 1/1 override (a copiable value).
 * Haste is *granted* to the created token through the [CREATED_TOKENS] pipeline — not a copiable
 * value (ruling) — lasting until your next turn.
 */
val OffspringsRevenge = card("Offspring's Revenge") {
    manaCost = "{2}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Enchantment"
    oracleText = "At the beginning of combat on your turn, exile target red, white, or black creature card " +
        "from your graveyard. Create a token that's a copy of that card, except it's 1/1. " +
        "It gains haste until your next turn."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creatureCard = target(
            TargetFilter(
                GameObjectFilter.Creature.withAnyColor(Color.RED, Color.WHITE, Color.BLACK).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Exile(creatureCard, fromZone = Zone.GRAVEYARD) then
            Effects.CreateTokenCopyOfTarget(
                target = creatureCard,
                overridePower = 1,
                overrideToughness = 1,
            ) then
            Effects.GrantKeyword(
                Keyword.HASTE,
                EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                Duration.UntilYourNextTurn,
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78619ffb-f514-4f9f-9d77-3ab49e045f7c.jpg?1783931020"
        ruling("2020-04-17", "The token copies exactly what was printed on the original card and nothing else, except the characteristics it specifically modifies. It doesn't copy any information about the object the card was before it was put into your graveyard.")
        ruling("2020-04-17", "The token's power and toughness are 1/1. These are copiable values of the token that other effects may copy. If the copied card has an ability that defines its power or toughness, that ability isn't copied. On the other hand, gaining haste after the token has been created is not a copiable value.")
        ruling("2020-04-17", "If the card copied by the token had any \"when [this permanent] enters the battlefield\" abilities, then the token also has those abilities and will trigger them when it's created. Similarly, any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities that the token has copied will also work.")
    }
}
