package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Transpose
 * {2}{B}
 * Instant
 * Draw a card, then discard a card. You lose 1 life. If this spell was cast from your hand, create
 * a 0/1 black Wizard creature token with "Whenever you cast a noncreature spell, this token deals
 * 1 damage to each opponent."
 * Rebound
 *
 * The token rider reads the resolving spell's cast origin ([Conditions.WasCastFromHand]), so the
 * rebound cast from exile on your next upkeep loots and drains but makes no Wizard.
 */
val Transpose = card("Transpose") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Draw a card, then discard a card. You lose 1 life. If this spell was cast from your hand, " +
        "create a 0/1 black Wizard creature token with \"Whenever you cast a noncreature spell, this token " +
        "deals 1 damage to each opponent.\"\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your " +
        "next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        effect = Patterns.Hand.loot() then
            Effects.LoseLife(1, EffectTarget.Controller) then
            Effects.If(
                condition = Conditions.WasCastFromHand,
                then = Effects.CreateToken(
                    power = 0,
                    toughness = 1,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Wizard"),
                    imageUri = "https://cards.scryfall.io/normal/front/1/8/187fe54c-7d0c-4225-9d46-3affbead897d.jpg?1783906133",
                    triggeredAbilities = listOf(
                        TriggeredAbility.create(
                            trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                            effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                        ),
                    ),
                ),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Toni Infante"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66392b0e-8691-42a4-bc84-03b017174a73.jpg?1783906356"
        ruling("2025-06-06", "The Wizard token's ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
        ruling("2025-06-06", "Casting the card again due to rebound's delayed triggered ability is optional. If you choose not to cast the card, or if you can't because an effect prohibits it, the card will stay exiled. You won't get another chance to cast it on a future turn. If you do cast the card, it's put into its owner's graveyard as normal once it resolves.")
        ruling("2025-06-06", "If a spell with rebound that you cast from your hand doesn't resolve for any reason, including being countered, that spell won't resolve and none of its effects will happen, including rebound. The spell will be put into its owner's graveyard and you won't get to cast it again on your next turn.")
    }
}
