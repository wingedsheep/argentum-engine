package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val RaulTroubleShooter = card("Raul, Trouble Shooter") {
    manaCost = "{1}{U}{B}"
    typeLine = "Legendary Creature — Zombie Mutant Rogue"
    power = 1
    toughness = 4
    oracleText = "Once during each of your turns, you may cast a spell from among cards in your graveyard that were milled this turn.\n" +
        "{T}: Each player mills a card. (They each put the top card of their library into their graveyard.)"

    // The once-per-turn allowance is tracked on Raul himself, so a casting through another
    // permission doesn't spend it and a new Raul brings a fresh one (rulings). Lands are never
    // cast, so the filter is nonland; "milled" excludes cards put there without the word mill.
    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Nonland.milledThisTurn(),
            duringYourTurnOnly = true,
            oncePerTurn = true
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
        description = "Each player mills a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Irina Nordsol"
        flavorText = "\"Raul Alfonso Tejada, former gunslinger and sole survivor of Hidalgo Ranch, at your service.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b73a314-b197-4954-a242-7d9096a406c1.jpg?1783912385"
        ruling(
            "2024-03-08",
            "If you are instructed to put a card into your graveyard from your library without using the word \"mill,\" Raul's first ability won't allow you to cast it."
        )
        ruling(
            "2024-03-08",
            "If you cast a spell from among cards in your graveyard that were milled this turn using another permission, Raul's effect doesn't apply. You can cast another spell among cards in your graveyard that were milled this turn using the permission granted by Raul's first ability."
        )
        ruling(
            "2024-03-08",
            "If you cast a spell from among cards in your graveyard that were milled this turn using the permission granted by Raul's first ability and then have a new Raul, Trouble Shooter come under your control in the same turn, you may cast another spell from among cards in your graveyard that were milled this turn."
        )
    }
}
