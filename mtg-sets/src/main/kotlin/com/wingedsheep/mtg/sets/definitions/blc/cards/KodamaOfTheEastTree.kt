package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.Effects

/**
 * Kodama of the East Tree
 * {4}{G}{G}
 * Legendary Creature — Spirit
 * 6/6
 *
 * Reach
 * Whenever another permanent you control enters, if it wasn't put onto the battlefield
 * with this ability, you may put a permanent card with equal or lesser mana value from
 * your hand onto the battlefield.
 * Partner (You can have two commanders if both have partner.)
 *
 * Implementation notes:
 * - Partner is omitted; the engine does not yet model multi-commander decks, and the
 *   Animated Army precon uses Bello as a single commander.
 * - The "if it wasn't put onto the battlefield with this ability" anti-loop guard is
 *   modeled by `MoveCollectionEffect.markEnteredViaSourceAbility` + the intervening-if
 *   `Conditions.TriggeringEntityWasNotPutByThisSource`: cards Kodama puts onto the
 *   battlefield are stamped with `EnteredViaAbilityComponent(thisKodamaId)`, and the
 *   trigger ignores entries carrying that marker for *this* Kodama. Another copy of
 *   Kodama still triggers off the same entry.
 */
val KodamaOfTheEastTree = card("Kodama of the East Tree") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spirit"
    power = 6
    toughness = 6
    oracleText = "Reach\n" +
        "Whenever another permanent you control enters, if it wasn't put onto the " +
        "battlefield with this ability, you may put a permanent card with equal or " +
        "lesser mana value from your hand onto the battlefield.\n" +
        "Partner (You can have two commanders if both have partner.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Any.youControl(),
            binding = TriggerBinding.OTHER,
        )
        // Anti-loop: the trigger ignores permanents that Kodama itself put onto the
        // battlefield. Set as an intervening-if (Rule 603.4) so the trigger never goes
        // on the stack for chained entries.
        triggerCondition = Conditions.TriggeringEntityWasNotPutByThisSource
        // The "you may" is modeled by `ChooseUpTo(1)` — the player can pick zero cards
        // to decline. No separate yes/no decision is required.
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.HAND,
                        player = Player.You,
                        filter = GameObjectFilter.Permanent.manaValueAtMostEntity(EntityReference.Triggering)
                    ),
                    storeAs = "kodama_candidates"
                ),
                SelectFromCollectionEffect(
                    from = "kodama_candidates",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "kodama_putting",
                    prompt = "Choose a permanent card with equal or lesser mana value to put onto the battlefield"
                ),
                MoveCollectionEffect(
                    from = "kodama_putting",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                    markEnteredViaSourceAbility = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ee1d489-0980-444e-8b07-107be791b76e.jpg?1721429321"
    }
}
