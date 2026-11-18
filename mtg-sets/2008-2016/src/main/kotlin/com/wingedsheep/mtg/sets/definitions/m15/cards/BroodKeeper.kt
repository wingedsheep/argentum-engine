package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brood Keeper
 * {3}{R}
 * Creature — Human Shaman
 * 2/3
 * Whenever an Aura becomes attached to this creature, create a 2/2 red Dragon creature token with
 * flying. It has "{R}: This creature gets +1/+0 until end of turn."
 *
 * The trigger watches the *host* side of the attach (the Bramble Elemental shape): any Aura,
 * whoever controls it, on every attach path — an Aura entering attached or an Aura already on the
 * battlefield moving onto Brood Keeper. `sourceItself()` on the attached-to filter pins the host.
 */
val BroodKeeper = card("Brood Keeper") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever an Aura becomes attached to this creature, create a 2/2 red Dragon creature token " +
        "with flying. It has \"{R}: This creature gets +1/+0 until end of turn.\""
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.withSubtype("Aura"))
            .becomesAttached(GameObjectFilter.Any.sourceItself())
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    cost = Costs.Mana("{R}")
                    effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
                    description = "{R}: This creature gets +1/+0 until end of turn."
                },
            ),
            imageUri = "https://cards.scryfall.io/normal/front/9/6/9661e23a-8914-493b-a08a-e9cd29b4666e.jpg?1783939141",
        )
        description = "Whenever an Aura becomes attached to this creature, create a 2/2 red Dragon creature " +
            "token with flying. It has \"{R}: This creature gets +1/+0 until end of turn.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Scott Murphy"
        flavorText = "\"Come, little one. Unfurl your wings, fill your lungs, and release your first fiery breath.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a640a5b-c638-4ae6-ad02-eb5fe20f4662.jpg?1783939177"
        ruling(
            "2014-07-18",
            "Brood Keeper's ability triggers both whenever an Aura enters the battlefield attached to Brood Keeper " +
                "and whenever an Aura on the battlefield attached to a different object becomes attached to Brood Keeper.",
        )
        ruling(
            "2014-07-18",
            "If an Aura becomes attached to Brood Keeper that causes another player to gain control of it, that " +
                "player will control the triggered ability that creates a Dragon.",
        )
    }
}
