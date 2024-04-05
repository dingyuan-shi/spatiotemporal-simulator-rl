from interfaces.maker import Maker

if __name__ == "__main__":
    # initialize a java based env
    env = Maker.make("warehouse")
    pickerInfo, availTasks, readyReturnRobot, availRobot = env.reset("synA")
    assignmment = '{4484:389}'
    path = '{389:{"s":0,"t":[{0:{"c":198,"r":98}},{1:{"c":198,"r":97}},{17:{"c":214,"r":97}},{18:{"c":214,"r":98}},{19:{"c":214,"r":97}},{20:{"c":214,"r":96}},{21:{"c":214,"r":95}},{24:{"c":214,"r":92}},{25:{"c":214,"r":91}},{26:{"c":214,"r":90}},{27:{"c":214,"r":89}},{30:{"c":214,"r":86}},{31:{"c":214,"r":85}},{32:{"c":214,"r":84}},{33:{"c":214,"r":83}},{36:{"c":214,"r":80}},{37:{"c":214,"r":79}},{38:{"c":214,"r":78}},{39:{"c":214,"r":77}},{42:{"c":214,"r":74}},{43:{"c":214,"r":73}},{44:{"c":214,"r":72}},{258:{"c":0,"r":72}},{259:{"c":0,"r":71}},{260:{"c":0,"r":70}}]}}'
    returnPath = '{}'
    observ, reward, done, info = env.step((returnPath, assignmment, path))
    print(observ, reward, done, info)
