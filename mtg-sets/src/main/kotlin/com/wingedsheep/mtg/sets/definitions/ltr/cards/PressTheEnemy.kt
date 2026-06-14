package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Press the Enemy — The Lord of the Rings: Tales of Middle-earth #65
 * {2}{U}{U} · Instant · Rare
 *
 * Return target spell or nonland permanent an opponent controls to its owner's hand. You may
 * cast an instant or sorcery spell with equal or lesser mana value from your hand without
 * paying its mana cost.
 *
 * Composed from existing pipeline primitives:
 *   1. Capture the target's mana value *before* the bounce — once a spell leaves the stack (or
 *      a permanent leaves the battlefield) the engine no longer tracks it as the same object,
 *      so the MV cap must be stored up front (`StoreNumber`).
 *   2. `ReturnSpellOrPermanentToOwnersHand` — one bounce that dispatches on the resolved target:
 *      a spell on the stack is removed from the stack to its owner's hand (it does not resolve,
 *      and this is not a counter), a permanent is bounced normally.
 *   3. Gather instant/sorcery cards in your hand, keep only those with MV ≤ the stored cap.
 *   4. `MayEffect(CastFromCollectionWithoutPayingCost)` — optional free cast; only prompted when
 *      a legal castable card exists, so a too-expensive spell is never offered.
 */
val PressTheEnemy = card("Press the Enemy") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target spell or nonland permanent an opponent controls to its owner's hand. " +
        "You may cast an instant or sorcery spell with equal or lesser mana value from your hand " +
        "without paying its mana cost."

    spell {
        val t = target(
            "target spell or nonland permanent an opponent controls",
            TargetSpellOrPermanent(
                permanentFilter = GameObjectFilter.NonlandPermanent.opponentControls()
            )
        )
        effect = Effects.Composite(
            listOf(
                // Capture the bounced object's mana value as the free-cast cap.
                Effects.StoreNumber(
                    "bouncedMv",
                    DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.ManaValue)
                ),
                // Return the spell or nonland permanent to its owner's hand.
                Effects.ReturnSpellOrPermanentToOwnersHand(t),
                // Gather instant/sorcery cards from your hand with MV ≤ the captured cap.
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.HAND,
                        player = Player.You,
                        filter = GameObjectFilter.InstantOrSorcery
                    ),
                    storeAs = "handSpells"
                ),
                FilterCollectionEffect(
                    from = "handSpells",
                    filter = CollectionFilter.ManaValueAtMost(DynamicAmount.VariableReference("bouncedMv")),
                    storeMatching = "castable"
                ),
                // You may cast one of them without paying its mana cost.
                ConditionalOnCollectionEffect(
                    collection = "castable",
                    ifNotEmpty = MayEffect(Effects.CastFromCollectionWithoutPayingCost("castable"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Valera Lutfullina"
        flavorText = "\"I showed the blade reforged to him. He is not so mighty yet that he is above fear.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfa5380a-480c-4c61-ac52-5debc49c5df9.jpg?1686968246"
    }
}
