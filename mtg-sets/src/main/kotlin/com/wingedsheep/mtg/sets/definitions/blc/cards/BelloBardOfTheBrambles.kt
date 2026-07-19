package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bello, Bard of the Brambles
 * {1}{R}{G}
 * Legendary Creature — Raccoon Bard
 * 3/3
 * During your turn, each non-Equipment artifact and non-Aura enchantment you control
 * with mana value 4 or greater is a 4/4 Elemental creature in addition to its other
 * types and has indestructible, haste, and "Whenever this creature deals combat
 * damage to a player, draw a card."
 *
 * Implementation: the animation is one printed static ability whose single continuous effect
 * spans Layer 4 (add creature type + Elemental subtype), Layer 6 (grant indestructible + haste),
 * and Layer 7b (set base 4/4). Per CR 613.6 those parts must affect the *same* locked-in set of
 * objects and keep applying even if Bello loses the ability mid-layer-sequence, so they are bundled
 * into one [CompositeStaticAbility] (gated by `IsYourTurn`) rather than separate `staticAbility`
 * blocks. The combat-damage trigger is granted separately — it is detected by TriggerDetector, not
 * projected through the layer system, and it can only fire when the source is a creature in combat,
 * which only happens on Bello's controller's turn (when the gating condition adds the creature type).
 */
val BelloBardOfTheBrambles = card("Bello, Bard of the Brambles") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Raccoon Bard"
    power = 3
    toughness = 3
    oracleText = "During your turn, each non-Equipment artifact and non-Aura enchantment you control with mana value 4 or greater is a 4/4 Elemental creature in addition to its other types and has indestructible, haste, and \"Whenever this creature deals combat damage to a player, draw a card.\""

    val animatedFilter = GroupFilter(
        (GameObjectFilter.Artifact.notSubtype(Subtype.EQUIPMENT)
            or GameObjectFilter.Enchantment.notSubtype(Subtype.AURA))
            .manaValueAtLeast(4)
            .youControl()
    )

    // One multi-layer static ability (CR 613.6): the type/subtype/keyword/P/T grants share a
    // single locked-in affected set and all persist even if Bello loses this ability in Layer 6.
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = CompositeStaticAbility(
            listOf(
                GrantCardType("CREATURE", animatedFilter),
                GrantSubtype("Elemental", animatedFilter),
                SetBasePowerToughnessStatic(4, 4, animatedFilter),
                GrantKeyword(Keyword.INDESTRUCTIBLE, animatedFilter),
                GrantKeyword(Keyword.HASTE, animatedFilter),
            )
        )
    }
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.DealsCombatDamageToPlayer.event,
                binding = Triggers.DealsCombatDamageToPlayer.binding,
                effect = Effects.DrawCards(1)
            ),
            filter = animatedFilter
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "1"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31e4b7a1-b377-49d2-a92e-4bcb0db35f16.jpg?1721428130"
    }
}
