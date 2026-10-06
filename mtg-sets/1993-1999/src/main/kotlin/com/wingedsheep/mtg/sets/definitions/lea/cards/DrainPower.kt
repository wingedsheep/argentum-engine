package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val DrainPower = card("Drain Power") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player activates a mana ability of each land they control. Then that player loses all unspent mana and you add the mana lost this way."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val lands = gather(CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.Land))
            run(Effects.ForEachInCollection(lands, Effects.ActivateManaAbility(EffectTarget.IterationEntity)))
        } then Effects.LoseUnspentMana(player, transferTo = EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea3830c5-cc66-453e-9e53-0636e00ee0ee.jpg?1783948706"
        ruling("2013-04-15", "A mana ability is an ability that (1) isn't a loyalty ability, (2) doesn't target, and (3) could add mana when it resolves.")
        ruling("2004-10-04", "This card forces the target player to draw mana from lands if they are untapped, but that player can choose how to draw the mana if a land they control has multiple mana abilities, or mana abilities with choices.")
        ruling("2004-10-04", "Your opponent may use instants and abilities of permanents in response to this spell before you get the mana.")
        ruling("2004-10-04", "If a land can draw a variable amount of mana, the target player (not the player of this spell) chooses how much to draw.")
    }
}
