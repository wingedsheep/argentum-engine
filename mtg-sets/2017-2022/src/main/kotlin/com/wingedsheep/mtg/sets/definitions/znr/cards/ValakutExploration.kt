package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Valakut Exploration
 * {2}{R}
 * Enchantment
 * Landfall — Whenever a land you control enters, exile the top card of your library. You may play
 * that card for as long as it remains exiled.
 * At the beginning of your end step, if there are cards exiled with this enchantment, put them into
 * their owner's graveyard, then this enchantment deals that much damage to each opponent.
 *
 * - **Landfall** exiles the top card *linked* to this enchantment (`linkToSource = true`, CR 607)
 *   and grants a [MayPlayExpiry.Permanent] play permission — "for as long as it remains exiled",
 *   so the permission outlives the enchantment (ruling) and ends when the card leaves exile.
 * - **The end-step trigger** is an intervening-if on the linked pile still holding cards (the
 *   Hostile Hostel count of `exiledWithSource()` cards in exile), re-checked on resolution. The
 *   pile is gathered at resolution, so a card played in response is neither binned nor counted
 *   (ruling); the damage is the number of cards actually gathered and binned.
 * - If the enchantment has left the battlefield the trigger doesn't exist, and the cards stay
 *   exiled and playable (ruling).
 */
val ValakutExploration = card("Valakut Exploration") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, exile the top card of your library. " +
        "You may play that card for as long as it remains exiled.\n" +
        "At the beginning of your end step, if there are cards exiled with this enchantment, put them " +
        "into their owner's graveyard, then this enchantment deals that much damage to each opponent."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.Pipeline {
            val valakutTop = gather(CardSource.TopOfLibrary(1))
            exile(valakutTop, linkToSource = true)
            run(Effects.GrantMayPlayFromExile(from = valakutTop, expiry = MayPlayExpiry.Permanent))
        }
        description = "Landfall — Whenever a land you control enters, exile the top card of your " +
            "library. You may play that card for as long as it remains exiled."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.Each, Zone.EXILE, GameObjectFilter.Any.exiledWithSource()),
            ComparisonOperator.GTE,
            1
        )
        effect = Effects.Pipeline {
            val valakutPile = gather(CardSource.FromLinkedExile())
            toGraveyard(valakutPile)
            run(
                Effects.DealDamage(
                    DynamicAmounts.distinctEntitiesIn(valakutPile),
                    EffectTarget.PlayerRef(Player.EachOpponent)
                )
            )
        }
        description = "At the beginning of your end step, if there are cards exiled with this " +
            "enchantment, put them into their owner's graveyard, then this enchantment deals that " +
            "much damage to each opponent."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18cb7bf6-9c7c-4e62-a678-7b75862e2f64.jpg?1783929344"
        ruling(
            "2020-09-25",
            "If you play the card in response to the end step triggered ability, that card won't be put " +
                "into your graveyard and won't be counted when determining how much damage to deal."
        )
        ruling(
            "2020-09-25",
            "You must follow the normal timing permissions and restrictions for the exiled card. If it's " +
                "a land, you can't play it unless you have land plays available."
        )
        ruling(
            "2020-09-25",
            "If Valakut Exploration leaves the battlefield, any cards exiled with it remain exiled and can " +
                "be played. The end step triggered ability won't trigger, and the cards won't be put into " +
                "their owner's graveyard."
        )
        ruling(
            "2020-09-25",
            "If you play a card this way, it leaves exile and becomes a new object. If it returns to exile " +
                "later in the turn, you can't play it again."
        )
    }
}
