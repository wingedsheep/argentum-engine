package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Mistway Spy — Murders at Karlov Manor #65
 * {U} · Creature — Merfolk Detective · 1/1
 *
 * Flying
 * Disguise {1}{U}
 * When this creature is turned face up, until end of turn, whenever a creature you control deals
 * combat damage to a player, investigate.
 *
 * A one-mana flier whose real cost is patience: cast face down for {3}, unmasked mid-combat for
 * {1}{U}, it converts an already-connecting board into a pile of Clues.
 *
 * The payoff is a *floating* triggered ability, not one the Spy has. That distinction is the whole
 * card: [Effects.CreateGlobalTriggeredAbility] with [Duration.EndOfTurn] creates an ability owned by
 * the game rather than by any permanent, so it keeps triggering for the rest of the turn even if the
 * Spy is killed in response to being turned face up, and it counts damage from *every* creature you
 * control — the Spy need not be attacking, or even alive.
 *
 * It repeats, so it is not a delayed trigger: each creature that connects triggers it separately, and
 * a first-strike creature that also deals regular damage triggers it twice. Hence the granted trigger
 * is `dealsDamage(Combat, AnyPlayer, Creature.youControl(), binding = ANY)` — the SDK's documented
 * shape for "whenever a creature you control deals combat damage to a player" — rather than the
 * SELF-bound [Triggers.DealsCombatDamageToPlayer], which would only ever see the Spy's own damage.
 *
 * Turning face up is a special action (CR 701.34c), not entering the battlefield, so this hangs off
 * [Triggers.TurnedFaceUp]. Face down the Spy is a colorless 2/2 with ward {2} and no flying, no
 * Detective type, and no trigger at all.
 */
val MistwaySpy = card("Mistway Spy") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Detective"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Disguise {1}{U} (You may cast this card face down for {3} as a 2/2 creature with ward {2}. " +
        "Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, until end of turn, whenever a creature you control " +
        "deals combat damage to a player, investigate."

    keywords(Keyword.FLYING)
    disguise = "{1}{U}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                trigger = Triggers.dealsDamage(
                    damageType = DamageType.Combat,
                    recipient = RecipientFilter.AnyPlayer,
                    sourceFilter = GameObjectFilter.Creature.youControl(),
                    binding = TriggerBinding.ANY
                ).event,
                binding = TriggerBinding.ANY,
                effect = Effects.Investigate()
            ),
            descriptionOverride = "Whenever a creature you control deals combat damage to a player, " +
                "investigate."
        )
        description = "When this creature is turned face up, until end of turn, whenever a creature " +
            "you control deals combat damage to a player, investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8578839-046f-4afd-a0e7-4737ded9e6eb.jpg?1783912908"

        ruling(
            "2024-02-02",
            "Mistway Spy's last ability triggers on combat damage dealt by any creature you control, " +
                "not just combat damage dealt by Mistway Spy."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "The resulting creature is a 2/2 creature with ward {2} that has no name, mana cost, or " +
                "creature types. Other effects that apply to the creature can still grant it any " +
                "characteristics it doesn't have."
        )
    }
}
