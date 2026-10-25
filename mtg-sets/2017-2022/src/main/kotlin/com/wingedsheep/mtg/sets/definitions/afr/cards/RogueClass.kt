package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Rogue Class {U}{B}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Whenever a creature you control deals combat damage to a player, exile the top card of that
 * player's library face down. You may look at it for as long as it remains exiled.
 *
 * {1}{U}{B}: Level 2
 * Creatures you control have menace.
 *
 * {2}{U}{B}: Level 3
 * You may play cards exiled with this Class, and you may spend mana as though it were mana of any
 * color to cast those spells.
 *
 * Level 1 is a per-creature combat-damage trigger (`Player.TriggeringPlayer` is the damaged
 * player, as on Virulent Silencer); the exile is face down, linked to this Class (CR 607 linked
 * abilities — level 3 names "cards exiled with this Class"), and `lookableInExile` grants the
 * Class's controller the look (Jacob Hauken's shape). Level 2 is Goblin War Drums' anthem. Level 3
 * is a `MayPlayCardsFromExile` static over the Class's own linked-exile pile (`exiledWithSource`,
 * evaluated with the Class as source), so every card the Class exiled — before or after it reached
 * level 3 — becomes playable, lands included ("play"), and each Rogue Class tracks only its own
 * pile, per the 2021-07-23 ruling.
 */
val RogueClass = card("Rogue Class") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "Whenever a creature you control deals combat damage to a player, exile the top card of that " +
        "player's library face down. You may look at it for as long as it remains exiled.\n" +
        "{1}{U}{B}: Level 2 — Creatures you control have menace.\n" +
        "{2}{U}{B}: Level 3 — You may play cards exiled with this Class, and you may spend mana as " +
        "though it were mana of any color to cast those spells."

    // Level 1: steal the top card of the damaged player's library, face down, for your eyes only.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val rogueTop = gather(CardSource.TopOfLibrary(1, player = Player.TriggeringPlayer))
            move(
                rogueTop,
                CardDestination.ToZone(Zone.EXILE, Player.TriggeringPlayer),
                faceDown = FaceDownMode.HIDDEN,
                linkToSource = true,
                lookableInExile = true
            )
        }
        description = "Whenever a creature you control deals combat damage to a player, exile the top " +
            "card of that player's library face down. You may look at it for as long as it remains exiled."
    }

    // Level 2: creatures you control have menace.
    classLevel(2, "{1}{U}{B}") {
        staticAbility {
            ability = GrantKeyword(Keyword.MENACE, GroupFilter(GameObjectFilter.Creature.youControl()))
        }
    }

    // Level 3: play the cards this Class exiled, with mana of any color.
    classLevel(3, "{2}{U}{B}") {
        staticAbility {
            ability = MayPlayCardsFromExile(
                filter = GameObjectFilter.Any.exiledWithSource(),
                withAnyManaColor = true
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Véronique Meignaud"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/0727f65b-cfbe-47d5-87c6-239cf8d93ca6.jpg?1783926444"
        ruling("2021-07-23", "If you have more than one Rogue Class on the battlefield, they each track exiled cards individually.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
    }
}
