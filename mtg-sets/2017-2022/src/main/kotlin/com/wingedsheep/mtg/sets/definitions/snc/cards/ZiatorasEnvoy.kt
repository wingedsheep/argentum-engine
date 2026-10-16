package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ziatora's Envoy
 * {1}{B}{R}{G}
 * Creature — Lizard Warrior
 * 5/4
 * Trample
 * Whenever this creature deals combat damage to a player, look at the top card of your library.
 *   You may play a land from the top of your library or cast a spell with mana value less than or
 *   equal to the damage dealt from the top of your library without paying its mana cost. If you
 *   don't, put that card into your hand.
 * Blitz {2}{B}{R}{G}
 *
 * The looked-at card is shown privately (Sunbird's Invocation's `showAllCards` choose-up-to-one),
 * with only a land or a nonland card of mana value ≤ the trigger's damage selectable. The pick is
 * played during resolution through Djinn of Wishes' [Effects.PlayFromCollectionWithoutPayingCost]:
 * a spell is cast for free, a land is played using up the land play (CR 305.2). Whatever is still
 * on top of the library afterwards — declined, ineligible, or an unplayable land — goes to hand
 * ("If you don't" covers both the land and the spell, per the ruling).
 */
val ZiatorasEnvoy = card("Ziatora's Envoy") {
    manaCost = "{1}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Creature — Lizard Warrior"
    power = 5
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, look at the top card of your " +
        "library. You may play a land from the top of your library or cast a spell with mana value " +
        "less than or equal to the damage dealt from the top of your library without paying its " +
        "mana cost. If you don't, put that card into your hand.\n" +
        "Blitz {2}{B}{R}{G}"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1, player = Player.You))
            val chosen = chooseUpTo(
                1,
                from = looked,
                filter = GameObjectFilter.Land or
                    GameObjectFilter.Nonland.manaValueAtMostDynamic(DynamicAmounts.triggerDamageAmount()),
                showAllCards = true,
                prompt = "You may play a land or cast a spell with mana value up to the damage dealt " +
                    "from the top of your library without paying its mana cost.",
                selectedLabel = "Play for free",
                remainderLabel = "Put into your hand"
            )
            run(Effects.PlayFromCollectionWithoutPayingCost(chosen))
            toHand(filter(looked, GameObjectFilter.Any.currentlyIn(Zone.LIBRARY)))
        }
        description = "Whenever this creature deals combat damage to a player, look at the top card " +
            "of your library. You may play a land from the top of your library or cast a spell with " +
            "mana value less than or equal to the damage dealt from the top of your library without " +
            "paying its mana cost. If you don't, put that card into your hand."
    }

    keywordAbility(KeywordAbility.blitz("{2}{B}{R}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba39154c-aace-4d55-a3ad-7c9eb717ed2b.jpg?1783923065"
        ruling(
            "2022-04-29",
            "\"If you don't\" in the second ability refers to whether you played a land or cast a " +
                "spell from the top of your library, not just whether or not you cast a spell."
        )
    }
}
