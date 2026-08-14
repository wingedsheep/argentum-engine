package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gandalf, Wandering Wizard — The Hobbit #41
 * {4}{U} · Legendary Creature — Avatar Wizard · Common
 * 4/5
 *
 * Ward {3}
 * {6}: Gandalf's owner shuffles him into their library and draws three cards.
 *
 * Modeling notes:
 *  - The ability names its *owner*, not its controller, so the draw uses
 *    [Player.OwnerOfSource] rather than the ambient controller. The two only diverge when Gandalf has
 *    been stolen: the thief may pay {6}, but it is Gandalf's owner who shuffles him back and draws.
 *  - Shuffle first, then draw — the printed order — so the three cards are drawn off a library that
 *    already contains him and he can genuinely be drawn again.
 */
val GandalfWanderingWizard = card("Gandalf, Wandering Wizard") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Avatar Wizard"
    power = 4
    toughness = 5
    oracleText = "Ward {3} (Whenever this creature becomes the target of a spell or ability an " +
        "opponent controls, counter it unless that player pays {3}.)\n" +
        "{6}: Gandalf's owner shuffles him into their library and draws three cards."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.ShuffleIntoLibrary(EffectTarget.Self) then
            Effects.DrawCards(3, EffectTarget.PlayerRef(Player.OwnerOfSource))
        description = "{6}: Gandalf's owner shuffles him into their library and draws three cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Irvin Rodriguez"
        flavorText = "\"May the wind under your wings bear you where the sun sails and the moon walks.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f8403a2-849c-4a59-b0ed-c8803995028d.jpg?1785496472"
    }
}
