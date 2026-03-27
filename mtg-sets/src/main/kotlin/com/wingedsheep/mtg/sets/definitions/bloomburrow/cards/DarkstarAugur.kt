package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Darkstar Augur
 * {2}{B}
 * Creature — Bat Warlock
 * 2/3
 *
 * Offspring {B} (You may pay an additional {B} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 *
 * Flying
 *
 * At the beginning of your upkeep, reveal the top card of your library and put
 * that card into your hand. You lose life equal to its mana value.
 */
val DarkstarAugur = card("Darkstar Augur") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Bat Warlock"
    power = 2
    toughness = 3
    oracleText = "Offspring {B} (You may pay an additional {B} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nFlying\nAt the beginning of your upkeep, reveal the top card of your library and put that card into your hand. You lose life equal to its mana value."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{B}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf()
    }

    keywords(Keyword.FLYING)

    // At the beginning of your upkeep, reveal top card → hand, lose life = its mana value
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1), Player.You),
                    storeAs = "revealed"
                ),
                MoveCollectionEffect(
                    from = "revealed",
                    destination = CardDestination.ToZone(Zone.HAND, Player.You),
                    revealed = true
                ),
                LoseLifeEffect(
                    DynamicAmount.StoredCardManaValue("revealed"),
                    EffectTarget.Controller
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c603751-1e2b-4c8e-a8d2-5c0876e7254f.jpg?1721426387"
    }
}
