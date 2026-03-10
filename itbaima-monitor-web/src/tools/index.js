function fitByUnit(value,unit){
    const units = ['B','KB','MB','GB','TB','PB'];
    let index = units.indexOf(unit);
    while(((value < 1 && value !== 0) || value > 1024) && (index >= 0 || index <= units.length)){
        if(value < 1024){
            value *= 1024
            index--
        }else{
            value /= 1024
            index++
        }
    }
    return `${parseInt(value)}${units[index]}`
}

export {fitByUnit};