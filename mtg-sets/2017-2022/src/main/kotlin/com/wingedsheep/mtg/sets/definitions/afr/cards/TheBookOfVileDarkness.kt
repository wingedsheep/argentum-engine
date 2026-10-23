package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Book of Vile Darkness
 * {B}{B}{B}
 * Legendary Artifact — Book
 * At the beginning of your end step, if you lost 2 or more life this turn, create a 2/2 black
 * Zombie creature token.
 * {T}, Exile The Book of Vile Darkness and artifacts you control named Eye of Vecna and Hand of
 * Vecna: Create Vecna, a legendary 8/8 black Zombie God creature token with indestructible and all
 * triggered abilities of the exiled cards.
 *
 * "The exiled cards" are all three — the Book's own end-step trigger included. Vecna is created
 * with those abilities as copiable values (the card's ruling), so Eye of Vecna's "when this enters"
 * draw triggers on Vecna's own entry.
 */
val TheBookOfVileDarkness = card("The Book of Vile Darkness") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Book"
    oracleText = "At the beginning of your end step, if you lost 2 or more life this turn, create a 2/2 " +
        "black Zombie creature token.\n{T}, Exile The Book of Vile Darkness and artifacts you control " +
        "named Eye of Vecna and Hand of Vecna: Create Vecna, a legendary 8/8 black Zombie God creature " +
        "token with indestructible and all triggered abilities of the exiled cards."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(DynamicAmounts.lifeLostThisTurn(), ComparisonOperator.GTE, 2)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.ExileSelf,
            // One of each: two artifacts named either, with different names.
            Costs.ExilePermanentsFixed(
                count = 2,
                filter = (GameObjectFilter.Artifact.named("Eye of Vecna") or
                    GameObjectFilter.Artifact.named("Hand of Vecna")).youControl(),
                distinctNames = true,
            ),
        )
        effect = Effects.Pipeline {
            val book = gather(CardSource.Entity(EffectTarget.Self))
            val relics = gather(CardSource.ExiledAsCost)
            run(
                Effects.CreateToken(
                    power = 8,
                    toughness = 8,
                    colors = setOf(Color.BLACK),
                    creatureTypes = setOf("Zombie", "God"),
                    keywords = setOf(Keyword.INDESTRUCTIBLE),
                    name = "Vecna",
                    legendary = true,
                    imageUri = "https://cards.scryfall.io/normal/front/8/6/86881c5f-df5e-4f50-b554-e4c49d5316f9.jpg?1783926347",
                    triggeredAbilitiesOf = listOf(book, relics),
                )
            )
        }
        description = "{T}, Exile The Book of Vile Darkness and artifacts you control named Eye of Vecna " +
            "and Hand of Vecna: Create Vecna, a legendary 8/8 black Zombie God creature token with " +
            "indestructible and all triggered abilities of the exiled cards."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "91"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05dcff61-8c48-401f-a31f-5fc53a298356.jpg?1783926499"
    }
}
