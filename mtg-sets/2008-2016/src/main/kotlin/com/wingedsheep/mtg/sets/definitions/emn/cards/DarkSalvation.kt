package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dark Salvation — Eldritch Moon #87
 * {X}{X}{B} · Sorcery
 *
 * Target player creates X 2/2 black Zombie creature tokens, then up to one target creature gets
 * -1/-1 until end of turn for each Zombie that player controls.
 *
 * The tokens are created for the targeted player (`controller = player`), then the Defile shape
 * runs: a resolution-time [Effects.ModifyStats] over the count of Zombie permanents *the targeted
 * player* controls. The count is evaluated when the second effect resolves, so it already sees the
 * X tokens just created. "Up to one" is an optional target — with none chosen, only the tokens
 * are made.
 */
val DarkSalvation = card("Dark Salvation") {
    manaCost = "{X}{X}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player creates X 2/2 black Zombie creature tokens, then up to one target " +
        "creature gets -1/-1 until end of turn for each Zombie that player controls."

    spell {
        val player = target(Targets.Player)
        val creature = target(TargetFilter.Creature, optional = true)
        val negZombies = -DynamicAmounts.battlefield(
            player.asPlayer,
            GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE)
        ).count()
        effect = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            controller = player,
            imageUri = "https://cards.scryfall.io/normal/front/1/5/15f1f43a-cbdb-4d1b-a254-dadcb2469784.jpg?1783937418"
        ) then Effects.ModifyStats(negZombies, negZombies, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Cynthia Sheppard"
        flavorText = "Countless ghouls surged through Thraben's streets, and with them came the city's salvation."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b5978bd-a696-430e-8c35-729d96eec53e.jpg?1783937485"
        ruling("2016-07-13", "You choose up to one target creature total, not a separate target for each Zombie that player controls.")
        ruling("2016-07-13", "If the targeted creature becomes an illegal target, the targeted player will still get Zombie tokens.")
        ruling("2016-07-13", "You may cast Dark Salvation with X as 0 just to have a creature get -1/-1 for each Zombie a player controls, and you may cast it without targeting any creature just to have a player get Zombie tokens. If you really just want to cast a spell, you can even cast it with X as 0 without a target creature and have neither effect do anything.")
    }
}
