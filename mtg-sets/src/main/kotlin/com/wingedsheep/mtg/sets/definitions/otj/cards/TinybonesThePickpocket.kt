package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tinybones, the Pickpocket
 * {B}
 * Legendary Creature — Skeleton Rogue
 * 1/1
 *
 * Deathtouch
 * Whenever Tinybones deals combat damage to a player, you may cast target nonland permanent
 * card from that player's graveyard, and mana of any type can be spent to cast that spell.
 *
 * Implementation:
 * - The combat-damage trigger (binding SELF, `RecipientFilter.AnyPlayer`) puts the ability on
 *   the stack, choosing a target nonland permanent card in an opponent's graveyard. Tinybones can
 *   only deal combat damage to an opponent, so "that player" is the (sole) opponent whose graveyard
 *   is scanned — modelled as a graveyard [TargetFilter] scoped to `ownedByOpponent()`.
 * - On resolution the body gathers the chosen target ([CardSource.ChosenTargets]) and grants a
 *   may-play-from-graveyard permission with `withAnyManaType = true`. The permission *is* the
 *   "you may cast" optionality (the player chooses whether to cast it), and `withAnyManaType`
 *   relaxes the spell's colored costs so mana of any type pays them — the same primitive Laughing
 *   Jasper Flint / Cruelclaw's Heist use. The card is not moved out of the graveyard; the
 *   enumerator already honours a `MayPlayPermission` on a graveyard card (Malcolm-style).
 * - Per the Scryfall ruling, the cast happens as the ability resolves and can't be deferred to
 *   later in the turn; the engine's established convention (Daring Waverider) models this
 *   "cast during resolution" window as a may-play permission expiring at end of turn.
 */
val TinybonesThePickpocket = card("Tinybones, the Pickpocket") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Skeleton Rogue"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\n" +
        "Whenever Tinybones deals combat damage to a player, you may cast target nonland " +
        "permanent card from that player's graveyard, and mana of any type can be spent to " +
        "cast that spell."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.AnyPlayer,
            binding = TriggerBinding.SELF,
        )
        target(
            "target nonland permanent card from that player's graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.NonlandPermanent.ownedByOpponent(),
                    zone = Zone.GRAVEYARD,
                )
            )
        )
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.ChosenTargets,
                    storeAs = "stolenCard",
                ),
                GrantMayPlayFromExileEffect(
                    from = "stolenCard",
                    expiry = MayPlayExpiry.EndOfTurn,
                    withAnyManaType = true,
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "109"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d3025a2-4a17-4137-ba0b-bd676c6f5f88.jpg?1712355685"

        ruling("2024-04-12", "You choose whether to cast the target nonland permanent card as " +
            "Tinybones's triggered ability resolves. If you do, you do so as part of the resolution " +
            "of that ability. You can't wait to cast it later in the turn. Timing restrictions based " +
            "on the card's types are ignored.")
    }
}
