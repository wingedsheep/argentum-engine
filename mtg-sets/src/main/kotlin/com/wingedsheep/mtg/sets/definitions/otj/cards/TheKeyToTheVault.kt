package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Key to the Vault
 * {1}{U}
 * Legendary Artifact — Equipment
 *
 * Whenever equipped creature deals combat damage to a player, look at that many cards
 * from the top of your library. You may exile a nonland card from among them. Put the
 * rest on the bottom of your library in a random order. You may cast the exiled card
 * without paying its mana cost.
 * Equip {2}{U}
 *
 * The trigger binds to the equipped creature ([TriggerBinding.ATTACHED]) and fires on a
 * combat-damage-to-a-player event; `DynamicAmount.ContextProperty(TRIGGER_DAMAGE_AMOUNT)`
 * captures "that many" (the amount of combat damage dealt). Modeled as the
 * Sunbird's-Invocation / Goliath-Daydreamer pipeline of atomic primitives:
 *
 *   1. [GatherCardsEffect] over the top N of your library — `revealed = false` because the
 *      card says "look at" (private), not "reveal".
 *   2. [SelectFromCollectionEffect] `ChooseUpTo(1)` with a nonland eligibility filter
 *      (`showAllCards = true` so lands are visible but unselectable); "you may exile".
 *   3. [MoveCollectionEffect] the chosen card to exile (`storeMovedAs` re-captures the moved
 *      entity), then the remainder to the bottom of the library in a random order.
 *   4. [CastFromCollectionWithoutPayingCostEffect] over the exiled card — "you may cast
 *      the exiled card without paying its mana cost", resolved during this ability so
 *      card-type timing is ignored (per ruling). If the player exiles nothing, the
 *      collections are empty and steps 3–4 are no-ops for them.
 */
val TheKeyToTheVault = card("The Key to the Vault") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Whenever equipped creature deals combat damage to a player, look at that " +
        "many cards from the top of your library. You may exile a nonland card from among " +
        "them. Put the rest on the bottom of your library in a random order. You may cast " +
        "the exiled card without paying its mana cost.\nEquip {2}{U}"

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            binding = TriggerBinding.ATTACHED
        )
        val damageDealt = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(count = damageDealt, player = Player.You),
                    storeAs = "keyLooked",
                    revealed = false
                ),
                SelectFromCollectionEffect(
                    from = "keyLooked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Nonland,
                    showAllCards = true,
                    storeSelected = "keyChosen",
                    storeRemainder = "keyToBottom",
                    prompt = "You may exile a nonland card to cast for free.",
                    selectedLabel = "Exile",
                    remainderLabel = "Put on the bottom of your library"
                ),
                MoveCollectionEffect(
                    from = "keyChosen",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.You),
                    storeMovedAs = "keyExiled"
                ),
                MoveCollectionEffect(
                    from = "keyToBottom",
                    destination = CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                    order = CardOrder.Random
                ),
                CastFromCollectionWithoutPayingCostEffect(from = "keyExiled")
            )
        )
    }

    equipAbility("{2}{U}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/166814af-a444-4a62-937e-7491673d9387.jpg?1712355445"

        ruling("2024-04-12", "You choose whether to cast the exiled card as The Key to the Vault's triggered ability resolves. If you do, you do so as part of the resolution of that ability. You can't wait to cast it later in the turn. Timing restrictions based on the card's types are ignored.")
        ruling("2024-04-12", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2024-04-12", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
