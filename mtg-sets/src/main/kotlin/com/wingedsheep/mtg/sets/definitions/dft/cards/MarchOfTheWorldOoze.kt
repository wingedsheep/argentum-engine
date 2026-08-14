package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * March of the World Ooze — Aetherdrift #169
 * {3}{G}{G}{G} · Enchantment
 *
 * Creatures you control have base power and toughness 6/6 and are Oozes in addition to their other
 * types.
 * Whenever an opponent casts a spell, if it's not their turn, you create a 3/3 green Elephant
 * creature token.
 *
 * The first line is *one* printed static ability spanning two Rule 613 layers — Layer 4 (the Ooze
 * subtype) and Layer 7b (base 6/6) — so per CR 613.6 both parts must apply to the same locked-in set
 * of objects. That is what [CompositeStaticAbility] is for (the Bello, Bard of the Brambles shape);
 * two separate `staticAbility { }` blocks would each re-resolve their own affected set per layer and
 * could drift apart. Setting *base* P/T (Layer 7b) rather than modifying it means +1/+1 counters and
 * lords still stack on top of the 6/6, and a later base-setting effect overwrites it.
 *
 * The trigger's "if it's not their turn" is an intervening-if (CR 603.4) on the *casting* player, not
 * on the controller — [Conditions.IsPlayersTurn] over [Player.TriggeringPlayer], the Scytheclaw
 * Raptor idiom. It is checked both when the ability would trigger and again on resolution, so an
 * opponent flashing something in during your turn makes a token while their own main-phase sorcery
 * does not.
 */
val MarchOfTheWorldOoze = card("March of the World Ooze") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have base power and toughness 6/6 and are Oozes in addition " +
        "to their other types.\n" +
        "Whenever an opponent casts a spell, if it's not their turn, you create a 3/3 green Elephant " +
        "creature token."

    val yourCreatures = GroupFilter(GameObjectFilter.Creature.youControl())

    // One multi-layer static (CR 613.6): the Layer 4 subtype grant and the Layer 7b base-P/T set
    // share a single locked-in affected set rather than each resolving their own.
    staticAbility {
        ability = CompositeStaticAbility(
            listOf(
                SetBasePowerToughnessStatic(6, 6, yourCreatures),
                GrantSubtype("Ooze", yourCreatures),
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.OpponentCastsSpell
        triggerCondition = Conditions.Not(Conditions.IsPlayersTurn(Player.TriggeringPlayer))
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elephant")
        )
        description = "Whenever an opponent casts a spell, if it's not their turn, you create a 3/3 " +
            "green Elephant creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "169"
        artist = "Helge C. Balzer"
        flavorText = "\"The fauna here functions as a planar antibody.\"\n—Rashmi, aether-seer"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1964ec5-0dd1-4b54-917c-cbeab05aba79.jpg?1783907868"
    }
}
