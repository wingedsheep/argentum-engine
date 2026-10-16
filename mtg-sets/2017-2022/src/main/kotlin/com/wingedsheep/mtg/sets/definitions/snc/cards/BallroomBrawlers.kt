package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ballroom Brawlers
 * {3}{W}{W}
 * Creature — Human Warrior
 * 3/5
 *
 * Whenever this creature attacks, this creature and up to one other target creature you control
 * both gain your choice of first strike or lifelink until end of turn.
 *
 * The choice is made once, on resolution, and applies to both creatures (see ruling), so it is a
 * single [ModalEffect.chooseOne] (`countsAsModalSpell = false` — a resolution-time choice, not a
 * modal ability) whose each mode grants its keyword to the Brawlers and to the optional "other"
 * target. A skipped optional target simply confers nothing on the second creature.
 */
val BallroomBrawlers = card("Ballroom Brawlers") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 5
    oracleText = "Whenever this creature attacks, this creature and up to one other target creature " +
        "you control both gain your choice of first strike or lifelink until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val other = target(TargetFilter(GameObjectFilter.Creature.youControl()).other(), optional = true)
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self, Duration.EndOfTurn) then
                    Effects.GrantKeyword(Keyword.FIRST_STRIKE, other, Duration.EndOfTurn),
                "First strike",
            ),
            Mode.noTarget(
                Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.Self, Duration.EndOfTurn) then
                    Effects.GrantKeyword(Keyword.LIFELINK, other, Duration.EndOfTurn),
                "Lifelink",
            ),
            countsAsModalSpell = false,
        )
        description = "Whenever this creature attacks, this creature and up to one other target " +
            "creature you control both gain your choice of first strike or lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "John Stanko"
        flavorText = "Don't try to cut in. They'll cut back."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf5615a2-03a2-41f4-ab3b-de5e60d1504e.jpg?1783923164"
        ruling(
            "2022-04-29",
            "You make the choice of first strike or lifelink only once, as Ballroom Brawlers's " +
                "triggered ability resolves. Both creatures gain the chosen ability."
        )
    }
}
