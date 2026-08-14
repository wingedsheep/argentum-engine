package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MultiplyManaOnSourceTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Virtue of Strength // Garenbrig Growth
 * {5}{G}{G}
 * Enchantment
 * If you tap a basic land for mana, it produces three times as much of that mana instead.
 *
 * Adventure: Garenbrig Growth — {G}, Sorcery — Adventure
 * Return target creature or land card from your graveyard to your hand.
 *
 * The enchantment half is the first *multiplicative* mana replacement in the engine, so it comes
 * with a new [MultiplyManaOnSourceTap] static — the multiplicative sibling of the existing additive
 * `AdditionalManaOnSourceTap` (Lavaleaper, Badgermole Cub), sharing its filter convention: the
 * filter is read from this enchantment's controller, so `BasicLand.youControl()` is exactly the
 * printed "**If you** tap a basic land" (only a permanent's controller can activate its mana
 * abilities).
 *
 * Three points the rulings pin down and the engine honors:
 *
 *  - **`{T}` is required.** You are "tapping a basic land for mana" only when you activate a mana
 *    ability whose cost includes the tap symbol, so an untapped mana ability is unaffected.
 *  - **Only the land's own ability scales.** A separate triggered mana ability that fires off the
 *    tap (Fertile Ground, Lavaleaper) produces its bonus outside this replacement.
 *  - **Copies are cumulative and multiplicative** — two Virtues of Strength make a basic land
 *    produce nine times as much, not six.
 *
 * The engine scales the resolving mana effect's amount rather than the pool afterwards, which keeps
 * restricted mana, spell riders and per-source provenance intact and makes the auto-tap solver
 * (which budgets off the same per-tap amount) and the client's `ManaAddedEvent` agree with it.
 */
val VirtueOfStrength = card("Virtue of Strength") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "If you tap a basic land for mana, it produces three times as much of that mana instead."

    staticAbility {
        ability = MultiplyManaOnSourceTap(
            sourceFilter = GameObjectFilter.BasicLand.youControl(),
            multiplier = 3
        )
    }

    adventure("Garenbrig Growth") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Return target creature or land card from your graveyard to your hand. (Then " +
            "exile this card. You may cast the enchantment later from exile.)"
        spell {
            val card = target(
                "target creature or land card in your graveyard",
                TargetObject(
                    filter = TargetFilter(
                        GameObjectFilter.CreatureOrLand.ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
            )
            effect = Effects.Move(card, Zone.HAND)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "197"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff857d41-767d-4e99-83cc-444738341b92.jpg?1783915075"

        ruling(
            "2023-09-01",
            "An object with the land card type and a basic land type has the intrinsic ability " +
                "\"{T}: Add [mana symbol],\" even if the text box doesn't actually contain that " +
                "text or the object has no text box. For example, a basic Forest will have the " +
                "intrinsic ability \"{T}: Add {G}.\""
        )
        ruling(
            "2023-09-01",
            "You're \"tapping a basic land for mana\" only if you're activating a mana ability of " +
                "a basic land that includes the {T} symbol in its cost. A mana ability produces " +
                "mana as part of its effect."
        )
        ruling(
            "2023-09-01",
            "If an ability triggers \"whenever you tap\" a basic land for mana and produces mana, " +
                "that triggered mana ability won't be affected by Virtue of Strength."
        )
        ruling(
            "2023-09-01",
            "Virtue of Strength doesn't produce any mana itself. Rather, it causes basic lands you " +
                "tap for mana to produce more mana."
        )
        ruling(
            "2023-09-01",
            "The effects of multiple copies of Virtue of Strength are cumulative. For example, if " +
                "you have two Virtue of Strengths on the battlefield, you'll get nine times the " +
                "original amount and type of mana."
        )
    }
}
