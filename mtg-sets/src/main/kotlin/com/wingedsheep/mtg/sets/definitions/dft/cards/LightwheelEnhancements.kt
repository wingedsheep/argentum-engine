package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Lightwheel Enhancements — Aetherdrift #20
 * {W} · Enchantment — Aura
 *
 * Enchant creature or Vehicle
 * Start your engines!
 * Enchanted permanent gets +1/+1 and has vigilance.
 * Max speed — You may cast this card from your graveyard.
 *
 * The buff half is the plain Aura shape (Silken Strength): [ModifyStats] plus a [GrantKeyword] auto-
 * targeted at the enchanted permanent, over a [GameObjectFilter.CreatureOrVehicle] `auraTarget` so it
 * can ride a Vehicle that isn't currently a creature.
 *
 * The recursion half is the interesting one. Per the Aetherdrift ruling, *"Max speed — [ability]"
 * means "As long as you have max speed, this object has [ability]" — and if the granted ability
 * functions in a zone other than the battlefield, the max speed ability does too.* So the permission
 * has to be live while the card sits in the graveyard, where it is not a permanent and the layer
 * system never sees it. [MayCastSelfFromZones] is exactly that: an intrinsic self-permission read
 * straight off the card definition by `CastFromZoneEnumerator` / `CastZoneResolver`, with its own
 * `condition` slot evaluated in the *casting player's* context — which the [maxSpeed] block folds
 * `YouHaveMaxSpeed` into rather than wrapping the ability in a `ConditionalStaticAbility` those raw
 * `filterIsInstance` read sites would never find.
 *
 * Nothing gates the cast beyond speed: it is cast for its normal mana cost at normal (sorcery) timing,
 * and dropping out of max speed revokes the permission mid-window because the condition is re-checked
 * when the cast is authorized.
 */
val LightwheelEnhancements = card("Lightwheel Enhancements") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or Vehicle\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "Enchanted permanent gets +1/+1 and has vigilance.\n" +
        "Max speed — You may cast this card from your graveyard."

    auraTarget = TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    startYourEngines()

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE)
    }

    maxSpeed {
        staticAbility {
            ability = MayCastSelfFromZones(listOf(Zone.GRAVEYARD))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ab169c1-4e25-4a5d-8961-4f06298c3781.jpg?1783907917"
        ruling(
            "2025-02-07",
            "\"Max speed — [ability]\" means \"As long as you have max speed, this object has " +
                "[ability].\" If the granted ability functions in a zone other than the battlefield, " +
                "the max speed ability does too."
        )
        ruling(
            "2025-02-07",
            "A player \"has max speed\" if their speed is 4."
        )
    }
}
