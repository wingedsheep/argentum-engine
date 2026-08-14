package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gonti, Night Minister
 * {2}{B}{B}
 * Legendary Creature — Aetherborn Rogue
 * 3/4
 *
 * Whenever a player casts a spell they don't own, that player creates a Treasure token.
 * Whenever a creature deals combat damage to one of your opponents, its controller looks at the
 * top card of that opponent's library and exiles it face down. They may play that card for as
 * long as it remains exiled. Mana of any type can be spent to cast a spell this way.
 *
 * Both abilities act on players other than Gonti's controller, so both read their actor out of
 * the trigger context rather than off the source.
 *
 * **Treasure trigger** — [SpellCastPredicate.NotOwnedByController] is the "a spell they don't own"
 * gate (owner vs. caster), and `Player.Each` widens it from the usual "whenever *you* cast" wording
 * to every seat. The Treasure goes to `Player.TriggeringPlayer` — the caster — not to Gonti's
 * controller. Gonti's own second ability is the main way an opponent ends up casting a card they
 * don't own, so the two halves feed each other.
 *
 * **Theft trigger** — an ANY-bound combat-damage observer (`RecipientFilter.Opponent` for "one of
 * your opponents", `sourceFilter = Creature` for "a creature"). Because the trigger carries a
 * source filter, the engine binds the *damaging creature* as the triggering entity and the
 * *damaged player* as the triggering player, which is exactly the pair the text needs:
 *  - the card comes off `Player.TriggeringPlayer`'s library ("that opponent's library") and is
 *    exiled face down ([FaceDownMode.HIDDEN]) — the same impulse-from-an-opponent's-library
 *    pipeline Black Cat, Cunning Thief and Laughing Jasper Flint use;
 *  - the play permission goes to [EffectTarget.ControllerOfTriggeringEntity] — "its controller",
 *    the damaging creature's controller, who may well be an opponent of Gonti's controller.
 *    `Permanent` expiry is "for as long as it remains exiled"; `withAnyManaType` is the last line.
 *
 * The gather uses [LookAudience.None] rather than the default `Controller`: the player who "looks"
 * is the grantee, not the ability's controller, and a face-down exiled card is already visible to
 * whoever holds a may-play permission on it (the same path that shows Black Cat's exiles). Showing
 * the default look overlay would instead leak the card to Gonti's controller.
 */
val GontiNightMinister = card("Gonti, Night Minister") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Aetherborn Rogue"
    power = 3
    toughness = 4
    oracleText = "Whenever a player casts a spell they don't own, that player creates a Treasure " +
        "token.\n" +
        "Whenever a creature deals combat damage to one of your opponents, its controller looks " +
        "at the top card of that opponent's library and exiles it face down. They may play that " +
        "card for as long as it remains exiled. Mana of any type can be spent to cast a spell " +
        "this way."

    triggeredAbility {
        trigger = Triggers.anyPlayerCasts(
            requires = setOf(SpellCastPredicate.NotOwnedByController),
        )
        effect = Effects.CreateTreasure(
            controller = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
        description = "Whenever a player casts a spell they don't own, that player creates a " +
            "Treasure token."
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.Opponent,
            sourceFilter = GameObjectFilter.Creature,
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    count = DynamicAmount.Fixed(1),
                    player = Player.TriggeringPlayer,
                ),
                storeAs = "stolenCard",
                lookAudience = LookAudience.None,
            ),
            MoveCollectionEffect(
                from = "stolenCard",
                destination = CardDestination.ToZone(Zone.EXILE, Player.TriggeringPlayer),
                faceDown = FaceDownMode.HIDDEN,
            ),
            GrantMayPlayFromExileEffect(
                from = "stolenCard",
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true,
                recipient = EffectTarget.ControllerOfTriggeringEntity,
            ),
        )
        description = "Whenever a creature deals combat damage to one of your opponents, its " +
            "controller looks at the top card of that opponent's library and exiles it face " +
            "down. They may play that card for as long as it remains exiled. Mana of any type " +
            "can be spent to cast a spell this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d79ca40a-e5c0-4956-8df0-ecbd2a25656f.jpg?1783907895"
    }
}
