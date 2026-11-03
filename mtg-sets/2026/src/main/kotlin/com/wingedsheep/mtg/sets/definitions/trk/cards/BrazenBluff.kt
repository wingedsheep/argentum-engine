package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brazen Bluff
 * {R}{R}
 * Instant
 * Choose target noncreature spell an opponent controls. That player may exile it. If they don't,
 * you may cast a noncreature spell from your hand without paying its mana cost.
 *
 * The yes/no is delegated to the targeted spell's controller ([EffectTarget.TargetController]);
 * declining runs the `otherwise` branch, where you choose up to one noncreature spell from your
 * hand and cast it during Brazen Bluff's resolution for free.
 */
val BrazenBluff = card("Brazen Bluff") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose target noncreature spell an opponent controls. That player may exile it. " +
        "If they don't, you may cast a noncreature spell from your hand without paying its mana cost."

    spell {
        target(TargetFilter(GameObjectFilter.Noncreature.opponentControls(), zone = Zone.STACK))
        effect = Effects.May(
            effect = Effects.ExileTargetSpell(),
            otherwise = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                val spellToCast = chooseSpell(
                    from = hand,
                    filter = GameObjectFilter.Nonland and GameObjectFilter.Noncreature,
                    prompt = "You may cast a noncreature spell from your hand without paying its mana cost",
                    selectedLabel = "Cast for free"
                )
                run(Effects.CastFromCollectionWithoutPayingCost(spellToCast))
            },
            decisionMaker = EffectTarget.TargetController,
            prompt = "Exile your spell? If you don't, your opponent may cast a noncreature spell from their hand for free."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Filipe Pagliuso"
        flavorText = "Some Starfleet captains played chess. Kirk preferred poker."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b306901-1bba-488b-866e-83aa162dab5a.jpg?1793274251"
    }
}
