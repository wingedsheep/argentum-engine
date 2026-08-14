package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hunted Bonebrute — Murders at Karlov Manor #87
 * {2}{B} · Creature — Skeleton Beast · 6/2
 *
 * Menace
 * When this creature enters, target opponent creates two 1/1 white Dog creature tokens.
 * When this creature dies, each opponent loses 3 life.
 * Disguise {1}{B}
 *
 * A six-power three-drop that hands the defender two chump blockers — the Hunted drawback, updated.
 * Disguise is the way out of the drawback: cast face down for {3} and flip for {1}{B}, and the
 * enters trigger never fires at all, because turning a permanent face up is not an
 * enters-the-battlefield event (CR 701.34c / the disguise rulings below). The dies trigger and
 * menace still apply once it is face up.
 *
 * The Dogs enter under the *targeted opponent's* control, which is what
 * [Effects.CreateToken]'s `controller` parameter expresses — the trigger's controller creates
 * nothing. If the targeted opponent has become an illegal target by resolution the whole trigger is
 * removed from the stack and no Dogs appear.
 *
 * The death trigger is `Player.EachOpponent`, not "target opponent" — in multiplayer every opponent
 * loses 3, including ones who never received a Dog.
 */
val HuntedBonebrute = card("Hunted Bonebrute") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton Beast"
    oracleText = "Menace\n" +
        "When this creature enters, target opponent creates two 1/1 white Dog creature tokens.\n" +
        "When this creature dies, each opponent loses 3 life.\n" +
        "Disguise {1}{B} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)"
    power = 6
    toughness = 2

    keywords(Keyword.MENACE)

    disguise = "{1}{B}"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Dog"),
            count = 2,
            controller = opponent,
        )
        description = "When this creature enters, target opponent creates two 1/1 white Dog " +
            "creature tokens."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "When this creature dies, each opponent loses 3 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4ca0e10-8b7c-4ce2-888b-752fc909757a.jpg?1783912897"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face " +
                "up, turning a permanent face up doesn't cause any enters-the-battlefield " +
                "abilities to trigger."
        )
    }
}
